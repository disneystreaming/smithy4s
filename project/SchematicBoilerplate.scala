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

  sealed trait BoilerplateModule extends Product with Serializable
  object BoilerplateModule {
    case object Core extends BoilerplateModule

    def templates: Map[BoilerplateModule, List[Template]] = Map(
      Core -> List(StructSyntax)
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


  object StructSyntax extends Template {
    override def filename(root: File): File =
      root / "generated" / "StructSyntax.scala"

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
        s"""def struct[S, ${`A..N`}]($schemaParams)(const : (${`A..N`}) => S) : Schema[S] =  Schema.StructSchema[S](placeholder, Hints.empty, Vector($args), arr => const($casts))"""

      block"""
      |package smithy4s
      |package schema
      |
      |trait StructSyntax {
      |
      |  protected def placeholder: ShapeId
      |
      |  def genericArityStruct[S](
      |      fields: SchemaField[S, _]*)(
      |      const: IndexedSeq[Any] => S) : Schema[S] =
      |    Schema.StructSchema(placeholder, Hints.empty, fields.toVector, const)
      |
      -  $smartCtsr
      |
      |}
      """
    }
  }

}
