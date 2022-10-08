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

package smithy4s
package dynamic

import model._

object Fixtures {

  val pizzaModelString =
    """|namespace foo
       |
       |service Service {
       |  operations: [Operation]
       |}
       |
       |@readonly
       |@http(method: "GET", uri: "/{name}")
       |operation Operation {
       |  input: Input,
       |  output: Output
       |}
       |
       |structure Input {
       |  @httpLabel
       |  @required
       |  name: String
       |}
       |
       |structure Output {
       |  greeting: String,
       |  someFloat: Float
       |}
       |""".stripMargin.trim

  val pizzaModel: Model = {
    Model(
      smithy = Some("2.0"),
      shapes = Map(
        IdRef("foo#Service") -> Shape.ServiceCase(
          ServiceShape(operations =
            Some(List(MemberShape(IdRef("foo#Operation"))))
          )
        ),
        IdRef("foo#Operation") -> Shape.OperationCase(
          OperationShape(
            input = Some(MemberShape(IdRef("foo#Input"))),
            output = Some(MemberShape(IdRef("foo#Output"))),
            traits = Some(
              Map(
                IdRef("smithy.api#http") -> Document.obj(
                  "method" -> Document.fromString("GET"),
                  "uri" -> Document.fromString("/{name}")
                ),
                IdRef("smithy.api#readonly") -> Document.obj()
              )
            )
          )
        ),
        IdRef("foo#Input") -> Shape.StructureCase(
          StructureShape(
            members = Some(
              Map(
                "name" -> MemberShape(
                  IdRef("smithy.api#String"),
                  traits = Some(
                    Map(
                      IdRef("smithy.api#httpLabel") -> Document.obj(),
                      IdRef("smithy.api#required") -> Document.obj()
                    )
                  )
                )
              )
            )
          )
        ),
        IdRef("foo#Output") -> Shape.StructureCase(
          StructureShape(
            members = Some(
              Map(
                "someFloat" -> MemberShape(
                  IdRef("smithy.api#Float")
                ),
                "greeting" -> MemberShape(
                  IdRef("smithy.api#String")
                )
              )
            )
          )
        )
      )
    )
  }

}
