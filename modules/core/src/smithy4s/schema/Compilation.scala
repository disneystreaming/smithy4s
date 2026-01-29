package smithy4s.schema

import smithy4s.{Lazy, ShapeId, Hints, Bijection, Refinement}
import smithy4s.schema.Schema.LazySchema
import Compilation.Visitor
import Schema._
import scala.collection.mutable.{Map => MMap}
import smithy4s.capability.EncoderK

/**
  * Applicative construct that allows to compositionally create programs that express schema compilation (ie, the process of creating codecs
  * described from schemas).
  *
  * The Applicative nature of this construct allows interesting patterns, such as:
  *   * aggressive caching of already-computed codecs,
  *   * control over recursions, as recursion is inherently deferred to the interpretation of the tree in this pattern.
  */
sealed trait Compilation[A] {
  def map[B](f: A => B) : Compilation[B] = Compilation.Mapped(this, f)
  def zip[B](other: Compilation[B]): Compilation[(A, B)] =
    Compilation.Sequenced(IndexedSeq(this.asInstanceOf[Compilation[Any]], other.asInstanceOf[Compilation[Any]])).map(seq => (seq(0).asInstanceOf[A], seq(1).asInstanceOf[B]))

  /**
    * Within a compilation tree, replaces occurrences of a visitor by another. It is helpful for overriding behaviour, as we are given
    * control over the delegating calls.
    */
  def replace[F[_]](initial: Visitor[F], replacement: Visitor[F]) : Compilation[A] = Compilation.replace(this, initial, replacement)
}

object Compilation {

  abstract class Deriving[F[_]](compiler: Visitor[F]) {
    private val cache = new MCache(scala.collection.concurrent.TrieMap.empty[Any, Any])
    def fromSchema[A](schema: Schema[A]) = unsafeInterpret(compiler.compile(schema), cache)
    implicit def derivedInstance[A](implicit schema: Schema[A]): F[A] = fromSchema(schema)
  }

  trait Visitor[F[_]] { self =>
    def primitive[P](shapeId: ShapeId, hints: Hints, tag: Primitive[P]): Compilation[F[P]]
    def collection[C[_], A](shapeId: ShapeId, hints: Hints, tag: CollectionTag[C], member: Schema[A]): Compilation[F[C[A]]]
    def map[K, V](shapeId: ShapeId, hints: Hints, key: Schema[K], value: Schema[V]): Compilation[F[Map[K, V]]]
    def enumeration[E](shapeId: ShapeId, hints: Hints, tag: EnumTag[E], values: List[EnumValue[E]], total: E => EnumValue[E]): Compilation[F[E]]
    def struct[S](shapeId: ShapeId, hints: Hints, fields: Vector[Field[S, _]], make: IndexedSeq[Any] => S): Compilation[F[S]]
    def union[U](shapeId: ShapeId, hints: Hints, alternatives: Vector[Alt[U, _]], ordinal: U => Int): Compilation[F[U]]
    def biject[A, B](schema: Schema[A], bijection: Bijection[A, B]): Compilation[F[B]]
    def refine[A, B](schema: Schema[A], refinement: Refinement[A, B]): Compilation[F[B]]
    def lazily[A](suspend: Lazy[Schema[A]]): Compilation[F[A]]
    def option[A](schema: Schema[A]): Compilation[F[Option[A]]]

    final def compile[A](schema: Schema[A]) : Compilation[F[A]] = Delegate(schema, self)
    /**
      * Creates a leaf expression containing a codec. Typically used for terminal nodes of a codec (such as primitives/enumerations)
      */
    final def leaf[A](fa: F[A]) : Compilation[F[A]] = Pure(fa)

    /**
      * Helper that must be used for the compilation of recursive codecs. It takes care of the safe traversal of the recursive tree
      * and prevents a number of foot-guns related to the lack of referentially-transparent computations in Scala.
      */
    final def buildRecursive[A](lazySchema: Lazy[Schema[A]])(buildRecursive: Lazy[F[A]] => F[A]) : Compilation[F[A]] = Cyclic(lazySchema, self, buildRecursive)

    /**
     * Allows to call upon a separate compiler to assist the implementation of this compiler.
     */
    final def delegate[G[_], A](schema: Schema[A], otherCompiler: Visitor[G]) : Compilation[G[A]] = Delegate(schema, otherCompiler)

    /**
     * Helper that should be used for the compilation of union codecs.
     */
    final def dispatch[U, Result](alternatives: Vector[Alt[U, _]], ordinal: U => Int, precompiler: Precompiler[F])(implicit encoderK: EncoderK[F, Result]) : Compilation[F[U]] = {
      Compilation.sequence(alternatives.map(alt => precompiler(alt.label, alt.schema.asInstanceOf[Schema[Any]]))).map {  codecs =>
        encoderK.absorb { (u: U) =>
          encoderK.apply(codecs(ordinal(u)), u)
        }
      }
    }
  }

  trait Precompiler[F[_]]{
    def apply[A](label: String, schema: Schema[A]) : Compilation[F[A]]
  }

  object Visitor {
    private[Compilation] def run[F[_], A](schema: Schema[A], visitor: Visitor[F]) : Compilation[F[A]] = {
    import visitor._
    schema match {
      case PrimitiveSchema(shapeId, hints, tag) => primitive(shapeId, hints, tag)
      case s: CollectionSchema[c, a] => collection[c,a](s.shapeId, s.hints, s.tag, s.member)
      case MapSchema(shapeId, hints, key, value) => map(shapeId, hints, key, value)
      case EnumerationSchema(shapeId, hints, tag, values, total) => enumeration(shapeId, hints, tag, values, total)
      case StructSchema(shapeId, hints, fields, make) => struct(shapeId, hints, fields, make)
      case UnionSchema(shapeId, hints, alts, ordinal) => union(shapeId, hints, alts, ordinal)
      case BijectionSchema(schema, bijection) => biject(schema, bijection)
      case RefinementSchema(schema, refinement) => refine(schema, refinement)
      case LazySchema(make) => lazily(make)
      case OptionSchema(a) => option(a)
    }
  }}


  def pure[A](a: A) : Compilation[A] = Pure(a)
  def sequence[A, B](seq: IndexedSeq[Compilation[A]]) : Compilation[IndexedSeq[A]] = Sequenced(seq)
  def zipN[A1, A2, A3](c1 : Compilation[A1], c2: Compilation[A2], c3: Compilation[A3]) : Compilation[(A1, A2, A3)] = {
    Compilation.Sequenced(IndexedSeq(c1.asInstanceOf[Compilation[Any]], c2.asInstanceOf[Compilation[Any]], c3.asInstanceOf[Compilation[Any]])).map(seq => (seq(0).asInstanceOf[A1], seq(1).asInstanceOf[A2], seq(2).asInstanceOf[A3]))
  }

  def compileSchema[F[_], A](schema: Schema[A], compiler: Visitor[F]) : Compilation[F[A]] = Compilation.Delegate(schema, compiler)

  /**
   * Runs the compilation by traversing its tree, producing and caching the necessary intermediate constructs that
   * participate in the construction of the value.
   *
   * This operation is inherently expensive in terms of allocations, and should be run wisely.
   */
  def expensiveRun[A](compilation: Compilation[A]) : A = {
    val mutableCache = new MCache(MMap.empty)
    unsafeInterpret(compilation, mutableCache)
  }

  private def replace[F[_], A](compilation: Compilation[A], initial: Visitor[F], replacement: Visitor[F]) : Compilation[A] = compilation match {
    case Pure(a) => Pure(a)
    case d : Delegate[f, a] if d.compiler == replacement => Delegate(d.schema, replacement.asInstanceOf[Visitor[f]])
    case d: Delegate[f, a] => d
    case c: Cyclic[f, a] if c.compiler == replacement => Cyclic(c.schema, replacement.asInstanceOf[Visitor[f]], c.buildRecursive)
    case c: Cyclic[f, a] => c
    case Mapped(ca, f) => Mapped(replace(ca, initial, replacement), f)
    case Sequenced(seq) => Sequenced(seq.map(replace(_, initial, replacement)))
  }

  private final case class Pure[A](a: A) extends Compilation[A]
  private final case class Delegate[F[_], A](schema: Schema[A], compiler: Visitor[F]) extends Compilation[F[A]]
  private final case class Cyclic[F[_], A](schema: Lazy[Schema[A]], compiler: Visitor[F], buildRecursive : Lazy[F[A]] => F[A]) extends Compilation[F[A]]
  private final case class Mapped[A, B](ca: Compilation[A], f: A => B) extends Compilation[B]
  private final case class Sequenced[A, B](seq: IndexedSeq[Compilation[A]]) extends Compilation[IndexedSeq[A]]


  private final class MCache(private[Compilation] val map: MMap[Any, Any]){
    def add[F[_], A](schema: Schema[A], compiler: Visitor[F], staged: F[A]) : Unit = {
      val _ = map.put((schema, compiler), staged)
    }
    def get[F[_], A](schema: Schema[A], compiler: Visitor[F]) : Option[F[A]] = {
      map.get((schema, compiler)).asInstanceOf[Option[F[A]]]
    }
  }

  /**
   * This runs a compilation against a mutable cache, storing the compiled codecs as the tree gets traversed.
   */
  private def unsafeInterpret[A](compilation: Compilation[A], mutableCache: MCache) : A = compilation match {
    case Pure(a) => a
    case Delegate(schema, compiler) =>
      mutableCache.get(schema, compiler) match {
        case Some(value) => value
        case None =>
          val result = unsafeInterpret(Visitor.run(schema, compiler), mutableCache)
          mutableCache.add(schema, compiler, result)
          result
      }
    case Cyclic(lschema, compiler, buildRecursive) =>
      val outerSchema = LazySchema(lschema)
      val innerSchema = lschema.value
      mutableCache.get(outerSchema, compiler) match {
        case Some(value) => value
        case None =>
          // We're creating an entry that contains a deferred codec, that will inspect the final cache when it's instantiated.
          val deferredCodec = buildRecursive(Lazy(mutableCache.get(innerSchema, compiler).get))
          mutableCache.add(outerSchema, compiler, deferredCodec)
          // At this point, the compilation cache contains the deferred entry. We can recurse safely, as the next traversal of
          // the `LazySchema` layer in the cycle will result in a cache-hit.
          unsafeInterpret(Delegate(innerSchema, compiler), mutableCache)
      }
    case Mapped(ca, f) => f(unsafeInterpret(ca, mutableCache))
    case Sequenced(seq) => seq.map(unsafeInterpret(_, mutableCache))
  }

}
