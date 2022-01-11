package smithy4s.dynamic.model

import weaver._
import smithy4s.Document
import software.amazon.smithy.model.{Model => SModel}
import software.amazon.smithy.model.shapes.ModelSerializer
import cats.effect.IO
import cats.syntax.all._

object DynamicModelSpec extends SimpleIOSuite {

  val modelString =
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
      |  greeting: String
      |}
      |""".stripMargin.trim

  val expected: Model = {
    Model(
      smithy = Some("1.0"),
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
                  "uri" -> Document.fromString("/{name}"),
                  "code" -> Document.fromInt(200)
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

  test("Decode json representation of models") {
    IO(
      SModel
        .assembler()
        .addUnparsedModel("foo.smithy", modelString)
        .assemble()
        .unwrap()
    ).map(ModelSerializer.builder().build.serialize(_))
      .map(NodeToDocument(_))
      .map(smithy4s.Document.decode[smithy4s.dynamic.model.Model](_))
      .flatMap(_.liftTo[IO])
      .map(obtained => expect.same(obtained, expected))
  }

}
