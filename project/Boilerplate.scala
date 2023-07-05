import sbt._
import scala.annotation.tailrec

/**
 * Copied, with some modifications, from https://github.com/milessabin/shapeless/blob/master/project/Boilerplate.scala
 *
 * Generate a range of boilerplate classes, those offering alternatives with 0-22 params
 * and would be tedious to craft by hand
 *
 * @author Miles Sabin
 * @author Kevin Wright
 */
object Boilerplate {
  import scala.StringContext._

  implicit final class BlockHelper(private val sc: StringContext)
      extends AnyVal {
    def block(args: Any*): String = {
      val interpolated = sc.standardInterpolator(treatEscapes, args)
      val rawLines = interpolated.split('\n')
      val trimmedLines = rawLines.map(_.dropWhile(_.isWhitespace))
      trimmedLines.mkString("\n")
    }
  }

  sealed trait ScalaVersion
  case object Scala2 extends ScalaVersion
  case object Scala3 extends ScalaVersion

  sealed trait BoilerplateModule extends Product with Serializable
  object BoilerplateModule {
    case object Core extends BoilerplateModule
    case object Core2 extends BoilerplateModule
    case object Core3 extends BoilerplateModule

    def templates: Map[BoilerplateModule, List[Template]] = Map(
      Core -> List(PartiallyAppliedStruct, PolyFunction),
      Core2 -> List(Scala2Kinds, FunctorK(Scala2)),
      Core3 -> List(Scala3Kinds, FunctorK(Scala3))
    )
  }

  /**
   * Returns a seq of the generated files.  As a side-effect, it actually generates them...
   */
  def gen(dir: File, module: BoilerplateModule) =
    for (t <- BoilerplateModule.templates(module)) yield {
      val tgtFile = t.filename(dir)
      IO.write(tgtFile, t.body)
      tgtFile
    }

  val maxArity = 22

  final class TemplateVals(val arity: Int) {
    val synTypes = (0 until arity).map(n => s"A$n")
    val synVals = (0 until arity).map(n => s"a$n")
    val synTypedVals =
      (synVals.zip(synTypes)).map { case (v, t) => v + ":" + t }
    val `A..N` = synTypes.mkString(", ")
    val `a..n` = synVals.mkString(", ")
    val `_.._` = Seq.fill(arity)("_").mkString(", ")
    val `?..?` = Seq.fill(arity)("?").mkString(", ")
    val `(A..N)` =
      if (arity == 1) "Tuple1[A]" else synTypes.mkString("(", ", ", ")")
    val `(_.._)` =
      if (arity == 1) "Tuple1[_]"
      else Seq.fill(arity)("_").mkString("(", ", ", ")")
    val `(a..n)` =
      if (arity == 1) "Tuple1(a)" else synVals.mkString("(", ", ", ")")
    val `a:A..n:N` = synTypedVals.mkString(", ")
    val `*` = IndexedSeq.fill(arity)("*").mkString(", ")
    val `Any..Any` = IndexedSeq.fill(arity)("Any").mkString(", ")
  }

  trait Template {
    def filename(root: File): File
    def content(tv: TemplateVals): String
    def range: IndexedSeq[Int] = 1 to maxArity

    val copyright =
      """/*
        | *  Copyright 2021-2022 Disney Streaming
        | *
        | *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
        | *  you may not use this file except in compliance with the License.
        | *  You may obtain a copy of the License at
        | *
        | *     https://disneystreaming.github.io/TOST-1.0.txt
        | *
        | *  Unless required by applicable law or agreed to in writing, software
        | *  distributed under the License is distributed on an "AS IS" BASIS,
        | *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        | *  See the License for the specific language governing permissions and
        | *  limitations under the License.
        | */
        |""".stripMargin

    def clean(lines: List[String]): List[String] = lines.toList match {
      case head1 :: head2 :: tl if (head1.isBlank() && head2.isBlank()) =>
        head1 :: tl
      case head :: tl => head :: clean(tl)
      case Nil        => Nil
    }

    def body: String = {
      def expandInstances(
          contents: IndexedSeq[Array[String]],
          acc: Array[String] = Array.empty
      ): Array[String] =
        if (!contents.exists(_.exists(_.startsWith("-"))))
          acc.map(_.tail)
        else {
          val pre = contents.head.takeWhile(_.startsWith("|"))
          val instances = contents.flatMap(
            _.dropWhile(_.startsWith("|")).takeWhile(_.startsWith("-"))
          )
          val next = contents.map(
            _.dropWhile(_.startsWith("|")).dropWhile(_.startsWith("-"))
          )
          expandInstances(next, acc ++ pre ++ instances)
        }

      val rawContents = range.map { n =>
        content(new TemplateVals(n)).split('\n').filterNot(_.isEmpty)
      }
      val headerLines = copyright.split('\n').toSeq ++ Seq(
        "",
        "/////// THIS FILE WAS GENERATED AT BUILD TIME, AND CHECKED-IN FOR DISCOVERABILITY ///////",
        ""
      )

      val instances = expandInstances(rawContents)
      val footerLines = rawContents.head.reverse
        .takeWhile(_.startsWith("|"))
        .map(_.tail)
        .reverse
      clean((headerLines ++ instances ++ footerLines).toList).mkString("\n")
    }
  }

  object PartiallyAppliedStruct extends Template {
    override def filename(root: File): File =
      root / "generated" / "schema" / "PartiallyAppliedStruct.scala"

    override def content(tv: TemplateVals): String = {
      import tv._

      val fields = synTypes.map { tpe =>
        s"Field[F, S, $tpe]"
      }

      val schemaFields = synTypes.map { tpe =>
        s"SchemaField[S, $tpe]"
      }

      val params =
        synVals.zip(fields).map { case (v, t) => s"$v: $t" }.mkString(", ")

      val schemaParams =
        synVals
          .zip(schemaFields)
          .map { case (v, t) => s"$v: $t" }
          .mkString(", ")

      val args = synVals.mkString(", ")

      val consArgs = synVals.map(v => s"_$v").mkString(", ")

      val structMethods =
        s"""def struct[S, ${`A..N`}]($params)(f: (${`A..N`}) => S): F[S]"""

      val casts = synTypes.zipWithIndex
        .map { case (a, i) =>
          s"arr($i).asInstanceOf[${a}]"
        }
        .mkString(", ")

      val smartCtsr =
        s"""def apply[${`A..N`}]($schemaParams)(const: (${`A..N`}) => S): Schema.StructSchema[S] = Schema.StructSchema[S](placeholder, Hints.empty, Vector($args), arr => const($casts))"""

      block"""
      |package smithy4s
      |package schema
      |
      |class PartiallyAppliedStruct[S] protected[schema](placeholder: ShapeId) {
      |
      |  def genericArity(
      |      fields: SchemaField[S, _]*)(
      |      const: IndexedSeq[Any] => S): Schema[S] =
      |    Schema.StructSchema(placeholder, Hints.empty, fields.toVector, const)
      |
      |  def apply(
      |     fields: Vector[SchemaField[S, _]])(
      |     const: IndexedSeq[Any] => S): Schema[S] =
      |    Schema.StructSchema(placeholder, Hints.empty, fields, const)
      |
      -  $smartCtsr
      |
      |}
      """
    }
  }
  object PolyFunction extends Template {
    override val range: IndexedSeq[Int] = IndexedSeq(1, 2, 5)
    override def filename(root: File): File =
      root / "generated" / "kinds" / "polyFunctions.scala"
    override def content(tv: TemplateVals): String = {
      import tv._

      val suffix = if (arity == 1) "" else arity.toString()

      block"""
      |package smithy4s
      |package kinds
      |
      |import smithy4s.Transformation
      |
      -trait PolyFunction$suffix[-F[${`_.._`}], +G[${`_.._`}]]{ self =>
      -   def apply[${`A..N`}](fa: F[${`A..N`}]): G[${`A..N`}]
      -
      -   final def andThen[H[${`_.._`}]](other: PolyFunction$suffix[G, H]): PolyFunction$suffix[F, H] = new PolyFunction$suffix[F, H]{
      -      def apply[${`A..N`}](fa: F[${`A..N`}]): H[${`A..N`}] = other(self(fa))
      -   }
      -
      -}
      -object PolyFunction$suffix{
      -  type From[F[${`_.._`}]] = {
      -    type Algebra[G[${`_.._`}]] = PolyFunction$suffix[F, G]
      -  }
      -
      -  def identity[F[${`_.._`}]]: PolyFunction$suffix[F, F] = new PolyFunction$suffix[F, F]{
      -    def apply[${`A..N`}](input: F[${`A..N`}]): F[${`A..N`}] = input
      -  }
      -
      -  implicit def polyfunction${suffix}_transformation[Alg[_[${`_.._`}]]: FunctorK$suffix, F[${`_.._`}], G[${`_.._`}]]: Transformation[PolyFunction$suffix[F, G], Alg[F], Alg[G]] =
      -    new Transformation[PolyFunction$suffix[F, G], Alg[F], Alg[G]]{
      -      def apply(func: PolyFunction$suffix[F, G], algF: Alg[F]): Alg[G] = FunctorK$suffix[Alg].mapK$suffix(algF, func)
      -    }
      -
      -   import Kind$arity._
      -   private[smithy4s] final def unsafeCacheBy[F[${`_.._`}], G[${`_.._`}], K](self: PolyFunction$suffix[F, G], allPossibleInputs: Seq[Existential[F]], getKey: Existential[F] => K): PolyFunction$suffix[F, G] =
      -     new PolyFunction$suffix[F, G] {
      -       private val map: Map[K, Any] = {
      -         val builder = Map.newBuilder[K, Any]
      -         allPossibleInputs.foreach(input =>
      -           builder += getKey(input) -> self
      -             .apply(input.asInstanceOf[F[${`Any..Any`}]])
      -             .asInstanceOf[Any]
      -         )
      -         builder.result()
      -       }
      -       def apply[${`A..N`}](input: F[${`A..N`}]): G[${`A..N`}] = map(getKey(existential(input))).asInstanceOf[G[${`A..N`}]]
      -   }
      -
      -   private[smithy4s] final def unsafeCache[F[${`_.._`}], G[${`_.._`}]](self: PolyFunction$suffix[F, G], allPossibleInputs: Seq[Existential[F]]): PolyFunction$suffix[F, G] =
      -     unsafeCacheBy[F, G, Existential[F]](self, allPossibleInputs, identity(_))
      -}
      -
      -
      |
      """
    }
  }

  case class FunctorK(scalaVersion: ScalaVersion) extends Template {
    override val range: IndexedSeq[Int] = IndexedSeq(1, 2, 5)
    override def filename(root: File): File =
      root / "generated" / "kinds" / "functorK.scala"
    override def content(tv: TemplateVals): String = {
      import tv._

      val suffix = if (arity == 1) "" else arity.toString()

      val functorKSig = scalaVersion match {
        case Scala2 => s"FunctorK$suffix[PolyFunction$suffix[F, *[${`_.._`}]]]"
        case Scala3 =>
          s"FunctorK$suffix[[G[${`_.._`}]] =>> PolyFunction$suffix[F, G]]"
      }

      val inline = scalaVersion match {
        case Scala2 => "@inline"
        case Scala3 => "inline"
      }

      block"""
      |package smithy4s
      |package kinds
      |
      -trait FunctorK$suffix[Alg[_[${`_.._`}]]] {
      -  def mapK$suffix[F[${`_.._`}], G[${`_.._`}]](alg: Alg[F], function: PolyFunction$suffix[F, G]): Alg[G]
      -}
      -object FunctorK$suffix {
      -  $inline def apply[Alg[_[${`_.._`}]]](implicit ev: FunctorK$suffix[Alg]): FunctorK$suffix[Alg] = ev
      -
      -  implicit def polyfunctionFunctorK$suffix[F[${`_.._`}]]: $functorKSig = new $functorKSig {
      -    def mapK$suffix[G[${`_.._`}], H[${`_.._`}]](fa: PolyFunction$suffix[F, G], fk: PolyFunction$suffix[G, H]): PolyFunction$suffix[F, H] = fa.andThen(fk)
      -  }
      -}
      -
      """
    }
  }

  object Scala3Kinds extends Template {
    override val range: IndexedSeq[Int] = IndexedSeq(1, 2, 5)
    override def filename(root: File): File =
      root / "generated" / "kinds" / "kinds.scala"
    override def content(tv: TemplateVals): String = {
      import tv._

      val suffix = if (arity == 1) "" else arity.toString()

      block"""
      |package smithy4s
      |package kinds
      |
      -object Kind$arity {
      -  type Existential[+F[${`_.._`}]] <: (Any { type T })
      -  inline def existential[F[${`_.._`}], ${`A..N`}](fa: F[${`A..N`}]): Existential[F] = fa.asInstanceOf[Existential[F]]
      -}
      -
      """
    }
  }

  object Scala2Kinds extends Template {
    override val range: IndexedSeq[Int] = IndexedSeq(1, 2, 5)
    override def filename(root: File): File =
      root / "generated" / "kinds" / "kinds.scala"
    override def content(tv: TemplateVals): String = {
      import tv._

      val suffix = if (arity == 1) "" else arity.toString()

      block"""
      |package smithy4s
      |package kinds
      |
      -object Kind$arity {
      -  type Existential[F[${`_.._`}]] = F[${`_.._`}]
      -  @inline def existential[F[${`_.._`}], ${`A..N`}](fa: F[${`A..N`}]): F[${`_.._`}] = fa.asInstanceOf[F[${`_.._`}]]
      -}
      -
      """
    }
  }

}
