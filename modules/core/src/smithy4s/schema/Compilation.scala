package smithy4s.schema

import smithy4s.Lazy
import smithy4s.schema.Schema.LazySchema
import Compilation.Compiler

/**
  * Applicative construct that allows to compositionally create programs expressing schema compilation.
  */
sealed trait Compilation[A] {
  def map[B](f: A => B) : Compilation[B] = Compilation.Mapped(this, f)
  def zip[B](other: Compilation[B]): Compilation[(A, B)] =
    Compilation.Sequenced(IndexedSeq(this.asInstanceOf[Compilation[Any]], other.asInstanceOf[Compilation[Any]])).map(seq => (seq(0).asInstanceOf[A], seq(1).asInstanceOf[B]))
  def replace[F[_]](initial: Compiler[F], replacement: Compiler[F]) : Compilation[A] = Compilation.replace(this, initial, replacement)
}

object Compilation {

  private type CompilationF[F[_], A] = Compilation[F[A]]
  type Compiler[F[_]] = SchemaVisitorBase[CompilationF[F, *]]

  trait Visitor[F[_]] extends Compiler[F]{ self =>
    final def compile[A](schema: Schema[A]) : Compilation[F[A]] = Delegate(schema, self)
    final def leaf[A](fa: F[A]) : Compilation[F[A]] = Pure(fa)
    final def buildRecursive[A](lazySchema: Lazy[Schema[A]])(buildRecursive: Lazy[F[A]] => F[A]) : Compilation[F[A]] = Cyclic(lazySchema, self, buildRecursive)
    final def delegate[G[_], A](schema: Schema[A], otherCompiler: Compiler[G]) : Compilation[G[A]] = Delegate(schema, otherCompiler)
  }

  def pure[A](a: A) : Compilation[A] = Pure(a)
  def sequence[A, B](seq: IndexedSeq[Compilation[A]]) : Compilation[IndexedSeq[A]] = Sequenced(seq)

  def compileSchema[F[_], A](schema: Schema[A], compiler: Compiler[F]) : Compilation[F[A]] = Compilation.Delegate(schema, compiler)
  def runFull[A](compilation: Compilation[A]) : A = {
    val (finalCache, staged) = interpret(compilation).run(Cache.empty)
    staged.run(finalCache)
  }

  private def replace[F[_], A](compilation: Compilation[A], initial: Compiler[F], replacement: Compiler[F]) : Compilation[A] = compilation match {
    case Pure(a) => Pure(a)
    case d : Delegate[f, a] if d.compiler == replacement => Delegate(d.schema, replacement.asInstanceOf[Compiler[f]])
    case d: Delegate[f, a] => d
    case c: Cyclic[f, a] if c.compiler == replacement => Cyclic(c.schema, replacement.asInstanceOf[Compiler[f]], c.buildRecursive)
    case c: Cyclic[f, a] => c
    case Mapped(ca, f) => Mapped(replace(ca, initial, replacement), f)
    case Sequenced(seq) => Sequenced(seq.map(replace(_, initial, replacement)))
  }

  private final case class Pure[A](a: A) extends Compilation[A]
  private final case class Delegate[F[_], A](schema: Schema[A], compiler: Compiler[F]) extends Compilation[F[A]]
  private final case class Cyclic[F[_], A](schema: Lazy[Schema[A]], compiler: Compiler[F], buildRecursive : Lazy[F[A]] => F[A]) extends Compilation[F[A]]
  private final case class Mapped[A, B](ca: Compilation[A], f: A => B) extends Compilation[B]
  private final case class Sequenced[A, B](seq: IndexedSeq[Compilation[A]]) extends Compilation[IndexedSeq[A]]


  private final case class Cache(private[Compilation] val map: Map[Any, Any]){
    def add[F[_], A](schema: Schema[A], compiler: Compiler[F], staged: Staged[F[A]]) : Cache = {
      println("#" * 30)
      println(map)
      new Cache(map + ((schema, compiler) -> staged))
    }
    def get[F[_], A](schema: Schema[A], compiler: Compiler[F]) : Option[Staged[F[A]]] = map.get((schema, compiler)).asInstanceOf[Option[Staged[F[A]]]]
  }
  private object Cache {
    val empty : Cache = new Cache(Map.empty)
  }

  private sealed trait Staged[A]{
    def flatMap[B](f: A => Staged[B]) : Staged[B]
    def map[B](f: A => B): Staged[B]
    def run(finalCache: Cache): A
  }

  private object Staged {
    final case class Eager[A](a: A) extends Staged[A] {
      def flatMap[B](f: A => Staged[B]): Staged[B] = f(a)
      def map[B](f: A => B): Staged[B] = Eager(f(a))
      def run(finalCache: Cache): A = a
    }
    case class Deferred[A](deferred : Cache => A) extends Staged[A] {
      def flatMap[B](f: A => Staged[B]): Staged[B] = Deferred(finalCache => f(this.run(finalCache)).run(finalCache))
      def map[B](f: A => B): Staged[B] = Deferred(finalCache => f(this.run(finalCache)))
      def run(finalCache: Cache): A = deferred(finalCache)
    }

    def eager[A](a: A) : Staged[A] = Eager(a)
    def deferred[A](f : Cache => A) : Staged[A] = Deferred(f)
    def sequence[A](seq: IndexedSeq[Staged[A]]) : Staged[IndexedSeq[A]] =
      if (seq.forall(_.isInstanceOf[Eager[_]])) Eager(seq.map(_.run(Cache.empty)))
      else Deferred(cache => seq.map(_.run(cache)))
  }

  private final case class State[A](run : Cache => (Cache, A)) {
    def flatMap[B](f : A => State[B]) : State[B] = State { cache0 =>
      val (cache1, a) = this.run(cache0)
      f(a).run(cache1)
    }
    def map[B](f: A => B) : State[B] = State { cache0 =>
      val (cache1, a) = this.run(cache0)
      (cache1, f(a))
    }
  }

  private object State {
    def current : State[Cache] = State(cache => (cache, cache))
    def pure[A](a: A) : State[A] = State(cache => (cache, a))
    def sequence[A](seq: IndexedSeq[State[A]]) : State[IndexedSeq[A]] = State { cache =>
      val builder = IndexedSeq.newBuilder[A]
      var currentCache = cache
      var i = 0
      while(i < seq.size){
        val (cache_i, a_i) = seq(i).run(currentCache)
        currentCache = cache_i
        builder.addOne(a_i)
        i += 1
      }
      (currentCache, builder.result())
    }
    def modify(f: Cache => Cache): State[Unit] = State(cache => (f(cache), ()))
  }

  /**
    * The result of this interpreter consists in 2 layers of monads :
    *   * a State monad that represents mutations of the compilation cache as we traverse schemas
    *   * an Ask monad that should feed on the final state of the compilation cache, once all schema layers
    *     have been traversed.
    */
  private def interpret[A](compilation: Compilation[A]) : State[Staged[A]] = compilation match {
    case Pure(a) => State.pure(Staged.eager(a))
    case Delegate(schema, compiler) =>
      State.current.flatMap {_.get(schema, compiler) match {
        case Some(value) => State.pure(value)
        case None =>
          for {
            result <- interpret(SchemaVisitorBase.run(schema, compiler))
            _ <- State.modify(_.add(schema, compiler, result))
          } yield result
      }
    }
    case Cyclic(lschema, compiler, buildRecursive) =>
      val outerSchema = LazySchema(lschema)
      val innerSchema = lschema.value
      // We're creating an entry that contains a deferred codec, that will inspect the final cache when it's instantiated.
      val recursiveEntry = Staged.deferred(cache => buildRecursive(Lazy(cache.get(innerSchema, compiler).get.run(cache))))
      State.current.flatMap {_.get(outerSchema, compiler) match {
        case Some(value) => State.pure(value)
        case None => for {
          _ <- State.modify(_.add(outerSchema, compiler, recursiveEntry))
          // At this point, the compilation cache contains the deferred entry. We can recurse safely, as the next traversal of
          // the `LazySchema` layer in the cycle will result in a cache-hit.
          result <- interpret(Delegate(innerSchema, compiler))
        } yield result
      }}
    case Mapped(ca, f) => interpret(ca).map(_.map(f))
    case Sequenced(seq) => State.sequence(seq.map(interpret(_))).map(Staged.sequence)
  }

}
