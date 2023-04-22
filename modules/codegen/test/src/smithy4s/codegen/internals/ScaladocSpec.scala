/*
 *  Copyright 2021-2022 Disney Streaming
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

package smithy4s.codegen.internals

final class ScaladocSpec extends munit.FunSuite {
  import TestUtils._

  test("Generate Scaladoc - include struct field docs") {
    val smithy =
      """
        |$version: "2"
        |
        |namespace smithy4s
        |
        |/// Struct docs
        |structure DocumentedStruct {
        |  /// int docs
        |  @required
        |  int: Integer
        |  /// struct docs
        |  struct: UndocumentedStruct
        |
        |  @required
        |  /// required before comment
        |  string: String
        |}
        |
        |structure UndocumentedStruct {
        |    /// boolean field
        |    boolean: Boolean
        |}
        |""".stripMargin

    ()
    val generatedCode = generateScalaCode(smithy)
    val documentedStructCode = generatedCode("smithy4s.DocumentedStruct")
    val undocumentedStructCode = generatedCode("smithy4s.UndocumentedStruct")

    assertContainsSection(documentedStructCode, "/** Struct docs")(
      """|/** Struct docs
         |  * @param int
         |  *   int docs
         |  * @param struct
         |  *   struct docs
         |  */
         |final case class DocumentedStruct(int: Int, string: String, struct: Option[UndocumentedStruct] = None)""".stripMargin
    )

    assertContainsSection(undocumentedStructCode, "/** @param boolean")(
      """|/** @param boolean
         |  *   boolean field
         |  */
         |final case class UndocumentedStruct(boolean: Option[Boolean] = None)""".stripMargin
    )

  }

  test("Generate Scaladoc - operation arguments") {
    val smithy =
      """
        |$version: "2"
        |
        |namespace smithy4s
        |
        |use smithy4s.meta#packedInputs
        |
        |service Service {
        |  operations: [HelloOp, PackedHelloOp]
        |}
        |
        |/// operation docs
        |operation HelloOp {
        |  input: Hello
        |}
        |
        |/// packed operation docs
        |@packedInputs
        |operation PackedHelloOp {
        |  input: Hello
        |}
        |
        |
        |/// the struct
        |structure Hello {
        |  /// the string
        |  @required
        |  string: String
        |  /// the int
        |  int: Integer
        |}
        |""".stripMargin

    val serviceCode = generateScalaCode(smithy)("smithy4s.Service")
    assertContainsSection(serviceCode, "/** operation docs")(
      """|/** operation docs
         |  * @param string
         |  *   the string
         |  * @param int
         |  *   the int
         |  */
         |def helloOp(string: String, int: Option[Int] = None): F[Hello, Nothing, Unit, Nothing, Nothing]""".stripMargin
    )

    assertContainsSection(serviceCode, "/** packed operation docs")(
      """|/** packed operation docs */
         |def packedHelloOp(input: Hello): F[Hello, Nothing, Unit, Nothing, Nothing]""".stripMargin
    )

  }

}
