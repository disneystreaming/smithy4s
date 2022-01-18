package smithy4s.dynamic.model

import weaver._
import software.amazon.smithy.model.{Model => SModel}
import software.amazon.smithy.model.shapes.ModelSerializer
import cats.effect.IO
import cats.syntax.all._
import java.nio.file.Paths
import smithy4s.Document
import smithy4s.dynamic
import smithy4s.http.HttpEndpoint
import smithy.api.Http

object DynamicModelPizzaSpec extends SimpleIOSuite {

  // This is not ideal, but it does the job.
  val cwd = System.getProperty("user.dir");
  val pizzaSpec = Paths.get(cwd + "/sampleSpecs/pizza.smithy").toAbsolutePath()
  val smithy4sPrelude = Paths
    .get(cwd + "/modules/protocol/resources/META-INF/smithy/smithy4s.smithy")
    .toAbsolutePath()

  test("Decode operation") {
    IO(
      SModel
        .assembler()
        .addImport(smithy4sPrelude)
        .addImport(pizzaSpec)
        .assemble()
        .unwrap()
    ).map(ModelSerializer.builder().build.serialize(_))
      .map(NodeToDocument(_))
      .map(smithy4s.Document.decode[smithy4s.dynamic.model.Model](_))
      .flatMap(_.liftTo[IO])
      .map { model =>
        expect(
          model.shapes(IdRef("smithy4s.example#Health")) == Shape.OperationCase(
            OperationShape(
              Some(MemberShape(IdRef("smithy4s.example#HealthRequest"), None)),
              Some(MemberShape(IdRef("smithy4s.example#HealthResponse"), None)),
              Some(
                List(
                  MemberShape(
                    IdRef("smithy4s.example#UnknownServerError"),
                    None
                  )
                )
              ),
              Some(
                Map(
                  IdRef(
                    "smithy.api#http"
                  ) -> Document.obj(
                    "code" -> Document.fromInt(200),
                    "method" -> Document.fromString("GET"),
                    "uri" -> Document.fromString("/health")
                  ),
                  IdRef("smithy.api#readonly") -> Document.obj()
                )
              )
            )
          )
        )
      }
  }

  test("Compile HTTP operation") {
    IO(
      SModel
        .assembler()
        .addImport(smithy4sPrelude)
        .addImport(pizzaSpec)
        .assemble()
        .unwrap()
    ).map(ModelSerializer.builder().build.serialize(_))
      .map(NodeToDocument(_))
      .map(smithy4s.Document.decode[smithy4s.dynamic.model.Model](_))
      .flatMap(_.liftTo[IO])
      .map { model =>
        val compiled = dynamic.Compiler.compile(model, Http)

        val endpoints = compiled.allServices.head.service.endpoints
        val httpEndpoints = endpoints.map(HttpEndpoint.cast(_))

        expect(
          httpEndpoints.forall(_.isDefined)
        )
      }
  }

}
