package smithy4s.codegen

import cats.implicits._
object Interpolator {
  class Interpolator(val sc: StringContext) extends AnyVal {
    def render(renderables: Renderable.WithValue[_]*):RenderResult = {
      renderAndCombine(renderables.toList)
    }

    private def renderAndCombine(renderables:List[ Renderable.WithValue[_]]): RenderResult ={
      def aux[A](binding: Renderable.WithValue[A]):RenderResult = binding.render
      val renderResults  =  renderables.map(r =>aux(r))
      println(sc.parts.size)
      sc.parts.toList.map(RenderResult(_))
        .zipAll(renderResults,RenderResult.empty,RenderResult.empty)
        .flatMap{case (a,b) => List(a,b)}
        .combineAll
    }
  }
}

trait RenderableInterpolator {
  implicit def renderResultInterpolator(sc: StringContext): Interpolator.Interpolator = new Interpolator.Interpolator(sc)
}

object test extends RenderableInterpolator with App {
import Renderable.WithValue._
implicit  val intRenderable:Renderable[Int] = (i) => RenderResult(Set.empty,List(s"$i"))

  val string = "hello"
  val int = 1
 // println(render"test $string more tests $int test")
  println(render"$string more tests $int")
}