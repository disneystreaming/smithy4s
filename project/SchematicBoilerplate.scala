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

  sealed trait SchematicModule extends Product with Serializable
  object SchematicModule {
    case object Core extends SchematicModule
    case object Scalacheck extends SchematicModule

    def templates: Map[SchematicModule, List[Template]] = Map(
      Core -> (Schemas.templates ++ Seq(Struct)),
      Scalacheck -> List(SchematicGenArity, DynamicSchema)
    )
  }

  /**
   * Returns a seq of the generated files.  As a side-effect, it actually generates them...
   */
  def gen(dir: File, module: SchematicModule) =
    for (t <- SchematicModule.templates(module)) yield {
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
    val `(A..N)` =
      if (arity == 1) "Tuple1[A]" else synTypes.mkString("(", ", ", ")")
    val `(_.._)` =
      if (arity == 1) "Tuple1[_]"
      else Seq.fill(arity)("_").mkString("(", ", ", ")")
    val `(a..n)` =
      if (arity == 1) "Tuple1(a)" else synVals.mkString("(", ", ", ")")
    val `a:A..n:N` = synTypedVals.mkString(", ")
  }

  trait Template {
    def filename(root: File): File
    def content(tv: TemplateVals): String
    def range = 1 to maxArity

    val copyright = """/*
                       | *  Copyright 2021 Disney Streaming
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
      val headerLines = copyright.split('\n').toSeq ++ Seq("")

      val instances = expandInstances(rawContents)
      val footerLines = rawContents.head.reverse
        .takeWhile(_.startsWith("|"))
        .map(_.tail)
        .reverse
      clean((headerLines ++ instances ++ footerLines).toList).mkString("\n")
    }
  }

  /*
    Blocks in the templates below use a custom interpolator, combined with post-processing to produce the body

      - The contents of the `header` val is output first

      - Then the first block of lines beginning with '|'

      - Then the block of lines beginning with '-' is replicated once for each arity,
        with the `templateVals` already pre-populated with relevant relevant vals for that arity

      - Then the last block of lines prefixed with '|'

    The block otherwise behaves as a standard interpolated string with regards to variable substitution.
   */
  object Struct extends Template {
    override def filename(root: File): File =
      root / "generated" / "struct.scala"

    override def content(tv: TemplateVals): String = {
      import tv._

      val fields = synTypes.map { tpe =>
        s"Field[F, Z, $tpe]"
      }

      val schemaFields = synTypes.map { tpe =>
        s"StructureField[S, Z, $tpe]"
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
        s"""def struct[Z, ${`A..N`}]($params)(f: (${`A..N`}) => Z): F[Z]"""

      val casts = synTypes.zipWithIndex
        .map { case (a, i) =>
          s"arr($i).asInstanceOf[${a}]"
        }
        .mkString(", ")

      val smartCtsrOpen =
        s"""def struct[S[x[_]] <: Schematic[x], Z, ${`A..N`}]($schemaParams)(const : (${`A..N`}) => Z) : schematic.Schema[S, Z] = new Schema[S, Z](Vector($args), arr => const($casts))"""

      val smartCtsrClosed =
        s"""def struct[Z, ${`A..N`}]($schemaParams)(const : (${`A..N`}) => Z) : schematic.Schema[S, Z] =  new Schema[S, Z](Vector($args), arr => const($casts))"""

      val compiledFields = synVals.map(_ + ".compile(s)").mkString(", ")

      block"""
      |package schematic
      |
      |object struct {
      |
      |  trait Schematic[F[_]] {
      |    def struct[S](fields: Vector[Field[F, S, _]])(const: Vector[Any] => S): F[S]
      |   }
      |
      |  trait OpenSyntax {
      |    def struct[S[x[_]] <: Schematic[x], Z](const: => Z) : schematic.Schema[S, Z] =
      |     new Schema(Vector.empty, _ => const)
      |
      |    def struct[S[x[_]] <: Schematic[x], Z](
      |      fields: Vector[StructureField[S, Z, _]])(
      |      const: Vector[Any] => Z) : schematic.Schema[S, Z] =
      |    new Schema(fields, const)
      |
      |    def bigStruct[S[x[_]] <: Schematic[x], Z](
      |      fields: StructureField[S, Z, _]*)(
      |       const: Vector[Any] => Z) : schematic.Schema[S, Z] =
      |    new Schema(fields.toVector, const)
      |
      -    $smartCtsrOpen
      |  }
      |
      |  trait ClosedSyntax[S[x[_]] <: Schematic[x]]{
      |    def struct[Z](const: => Z) : schematic.Schema[S, Z] =
      |     new Schema(Vector.empty, _ => const)
      |
      |    def struct[Z](
      |      fields: Vector[StructureField[S, Z, _]])(
      |      const: Vector[Any] => Z) : schematic.Schema[S, Z] =
      |    new Schema(fields, const)
      |
      |    def bigStruct[Z](
      |      fields: StructureField[S, Z, _]*)(
      |      const: Vector[Any] => Z) : schematic.Schema[S, Z] =
      |    new Schema(fields.toVector, const)
      |
      -    $smartCtsrClosed
      |  }
      |
      |   class Schema[S[x[_]] <: Schematic[x], Z](
      |     fields: Vector[StructureField[S, Z, _]],
      |     const: Vector[Any] => Z
      |   ) extends schematic.Schema[S, Z]{
      |     def compile[F[_]](s: S[F]) : F[Z] = s.struct(fields.map(_.compile(s)))(const)
      |   }
      |
      |}
      """
    }
  }

  object DynamicSchema extends Template {
    override def filename(root: File): File =
      root / "generated" / "DynSchemaArity.scala"

    override def content(tv: TemplateVals): String = {
      import tv._

      val fields = synTypes.zipWithIndex
        .map(_._2)
        .map(index => s"v($index)")
        .mkString(", ")

      val params = synVals.mkString(", ")

      val tuples = synVals.zipWithIndex
        .map { case (a, index) => s"v($index).label -> $a" }
        .mkString(", ")

      val patterns =
        s"case v if v.size == $arity => struct($fields)(($params) => dynStruct($tuples))"

      block"""
      |package schematic
      |package scalacheck
      |
      |trait DynSchemaArity[S[x[_]] <: SchemaGenerator.DefaultMetamodel[x]] { self : SchemaGenerator[S] =>
      |  private object syntax extends struct.ClosedSyntax[S]
      |  import syntax._
      |
      |  def dynStruct(fields : Vector[DynFieldSchema]) : DynSchema = (fields match {
      -    $patterns
      |    case _ => struct(fields.toVector){ values =>  dynStruct(fields.map(_.label).zip(values):_*) }.asInstanceOf[DynSchema]
      |  }).asInstanceOf[DynSchema]
      |}
      """
    }
  }

  object SchematicGenArity extends Template {
    override def filename(root: File): File =
      root / "generated" / "SchematicGenArity.scala"

    override def content(tv: TemplateVals): String = {
      import tv._

      val fields = synTypes.map { tpe =>
        s"Field[Gen, Z, $tpe]"
      }

      val params =
        synVals.zip(fields).map { case (v, t) => s"$v: $t" }.mkString(", ")

      val gens = synVals.map(a => s"genField($a)").mkString(", ")

      val f = if (arity == 1) "f" else "f.tupled"

      val structMethods =
        s"""def struct[Z, ${`A..N`}]($params)(f: (${`A..N`}) => Z): Gen[Z] = Gen.zip($gens).map($f)"""

      block"""
        |package schematic
        |package scalacheck
        |
        |import org.scalacheck.Gen
        |
        |trait SchematicGenArity extends struct.Schematic[Gen] {
        |
        |  protected def genField[S, A](field: Field[Gen, S, A]): Gen[A] = Gen.lzy(field.instanceA {
        |    new Field.ToOptional[Gen] { def apply[AA](genA: Gen[AA]) : Gen[Option[AA]] = Gen.option(genA) }
        |  })
        |
        -  $structMethods
        |
        |}
        """
    }
  }

  object Schemas {

    val primitives = List(
      "Boolean",
      "Byte",
      "Short",
      "Int",
      "Float",
      "Double",
      "scala.math.BigInt",
      "scala.math.BigDecimal",
      "Long",
      "String",
      "Unit",
      "java.util.UUID"
    )

    val collections = List(
      "List",
      "Set",
      "Vector"
    )

    val templates: List[Template] =
      primitives.map(PrimitiveTemplate(_)) ++
        collections.map(CollectionTemplate(_))

    abstract class SchemaTemplate(qualifiedTyp: String) extends Template {
      def content(): String

      def filename(root: File) = {
        val fn = qualifiedTyp.split('.').last.toLowerCase() + ".scala"
        root / "generated" / fn
      }

      def content(tv: TemplateVals) = ""
      override def body: String = this.copyright + "\n" + content()
    }

    case class PrimitiveTemplate(qualifiedTyp: String)
        extends SchemaTemplate(qualifiedTyp) {

      def content() = {
        val split = qualifiedTyp.split('.')
        val typ = split.last
        val imports =
          if (split.size == 1) ""
          else s"\nimport $qualifiedTyp\n"
        val lower = typ.toLowerCase()

        s"""
          |package schematic
          |
          |$imports
          |object $lower {
          |
          |  object Schema extends schematic.Schema[Schematic, $typ] {
          |    def compile[F[_]](s: Schematic[F]): F[$typ] = s.$lower
          |  }
          |
          |  trait Schematic[F[_]] {
          |    def $lower: F[$typ]
          |  }
          |
          |  trait Syntax {
          |    val $lower : schematic.Schema[Schematic, $typ] = Schema
          |  }
          |
          |}
        """.stripMargin.trim() + "\n"
      }
    }

    case class CollectionTemplate(qualifiedTyp: String)
        extends SchemaTemplate(qualifiedTyp) {

      def content() = {
        val split = qualifiedTyp.split('.')
        val typ = split.last
        val imports =
          if (split.size == 1) ""
          else s"\nimport $qualifiedTyp\n"
        val lower = typ.toLowerCase()

        s"""
          |package schematic
          |
          |object $lower {
          |
          |  class Schema[S[x[_]] <: Schematic[x], A](
          |      a: schematic.Schema[S, A])
          |      extends schematic.Schema[S, $typ[A]] {
          |    def compile[F[_]](s: S[F]): F[$typ[A]] =
          |      s.$lower(a.compile(s))
          |  }
          |
          |  trait Schematic[F[_]] {
          |    def $lower[S](fs: F[S]): F[$typ[S]]
          |  }
          |
          |  trait OpenSyntax {
          |    def $lower[S[x[_]] <: Schematic[x], A](
          |        a: schematic.Schema[S, A]): schematic.Schema[S, $typ[A]] =
          |      new Schema[S, A](a)
          |  }
          |
          |  trait ClosedSyntax[S[x[_]] <: Schematic[x]] {
          |    def $lower[A](
          |        a: schematic.Schema[S, A]): schematic.Schema[S, $typ[A]] =
          |      new Schema[S, A](a)
          |  }
          |}
        """.stripMargin.trim() + "\n"
      }
    }

  }

}
