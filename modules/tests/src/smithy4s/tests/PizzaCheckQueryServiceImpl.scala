/*
 *  Copyright 2021-2025 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
  ): IO[CheckQueryOutput] = output(inp, kind = Some("y"), variant = Some(""))

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

  def checkQueryKindZVariantA(
      inp: Map[String, List[String]]
  ): IO[CheckQueryOutput] = output(inp, variant = Some("a"), kind = Some("z"))
}
