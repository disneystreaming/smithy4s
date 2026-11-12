import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}
import software.amazon.smithy.build.{ProjectionTransformer, TransformContext}
import software.amazon.smithy.model.Model

class CountingTransformer extends ProjectionTransformer {
  def getName(): String = "count-generation"

  def transform(context: TransformContext): Model = {
    Files.write(
      Paths.get(System.getProperty("smithy4s.test.counter")),
      "run\n".getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE,
      StandardOpenOption.APPEND
    )
    context.getModel()
  }
}
