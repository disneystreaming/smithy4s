package smithy4s

import smithy4s.dynamic.DummyIO._
import java.nio.file.Paths
import software.amazon.smithy.model.{Model => SModel}
import software.amazon.smithy.model.shapes.ModelSerializer
import cats.syntax.all._

package object dynamic {
  // This is not ideal, but it does the job.
  private val cwd = System.getProperty("user.dir");

  def loadDynamicModel(specName: String) =
    IO {
      val spec = Paths.get(cwd + s"/sampleSpecs/$specName").toAbsolutePath()
      SModel
        .assembler()
        .discoverModels()
        .addImport(spec)
        .assemble()
        .unwrap()
    }
      .map(ModelSerializer.builder().build.serialize(_))
      .map(NodeToDocument(_))
      .map(smithy4s.Document.decode[smithy4s.dynamic.model.Model](_))
      .flatMap(_.liftTo[IO])

}
