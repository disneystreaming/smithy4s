package smithy4s.tests
import cats.effect._

import smithy4s.example._

trait PizzaCheckQueryServiceImpl {
  private def output(
      inp: Map[String, List[String]],
      variant: Option[String] = None,
      kind: Option[String] = None
  ): IO[CheckQueryOutput] =
    IO.pure(
      CheckQueryOutput(
        variants = inp.getOrElse("variant", Nil),
        staticVariants = variant.toList,
        kinds = inp.getOrElse("kind", Nil),
        staticKinds = kind.toList
      )
    )


  def checkQueryKindZ(inp: Map[String, List[String]]): IO[CheckQueryOutput] =
    output(inp, kind = Some("z"))

  def checkQueryKindYVariant(
      inp: Map[String, List[String]]
  ): IO[CheckQueryOutput] = output(inp, kind = Some("y"), variant =Some(""))

  def checkQueryKindXVariantC(
      inp: Map[String, List[String]]
  ): IO[CheckQueryOutput] = output(inp, kind = Some("x"), variant = Some("c"))

  def checkQueryKindXVariantD(
      inp: Map[String, List[String]]
  ): IO[CheckQueryOutput] = output(inp, kind = Some("x"), variant = Some("d"))

  def checkQueryVariantA(inp: Map[String, List[String]]): IO[CheckQueryOutput] =
    output(inp, variant = Some("a"))

  def checkQueryVariantB(
      inp: Map[String, List[String]]
  ): IO[CheckQueryOutput] = output(inp, variant = Some("b"))
}