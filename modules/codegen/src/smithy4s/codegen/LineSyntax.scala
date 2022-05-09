package smithy4s.codegen

import cats.implicits._
import smithy4s.codegen.WithValue.ToLineWithValue

object LineSyntax {
  implicit class LineInterpolator(val sc: StringContext) extends AnyVal {
    def line(renderables: ToLineWithValue[_]*): Line = {
      renderAndCombine(renderables.toList)
    }

    private def renderAndCombine(
        renderables: List[ToLineWithValue[_]]
    ): Line = {
      def aux[A](binding: ToLineWithValue[A]): Line = {
        val (imports, lines) = binding.render.tupled
        Line(imports, lines.mkString(""))
      }
      val renderLines: List[Line] = renderables.map(r => aux(r))
      sc.parts.toList
        .map(Line(_))
        .zipAll(renderLines, Line.empty, Line.empty)
        .flatMap { case (a, b) => List(a, b) }
        .combineAll
    }

  }
}

