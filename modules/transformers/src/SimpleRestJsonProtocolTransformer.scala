package smithy4s.transformers

import alloy.SimpleRestJsonTrait
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.build.{ProjectionTransformer, TransformContext}
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.{Shape, ShapeId}
import software.amazon.smithy.model.traits.Trait
import software.amazon.smithy.protocoltests.traits.{HttpRequestTestCase, HttpRequestTestsTrait, HttpResponseTestCase, HttpResponseTestsTrait}
import java.util.function.BiFunction
import scala.jdk.CollectionConverters.{CollectionHasAsScala, SeqHasAsJava}
final class SimpleRestJsonProtocolTransformer extends ProjectionTransformer {
  override def getName: String = "ProtocolTransformer"

  def transform(ctx: TransformContext): Model = {
    val traitMapper: BiFunction[Shape, Trait, Trait] = (_: Shape, theTrait: Trait) => {
      theTrait match {
        case _:RestJson1Trait => new SimpleRestJsonTrait()
        case c: HttpRequestTestsTrait => new HttpRequestTestsTrait(c.getSourceLocation, c.getTestCases.asScala.toList.map {
          case req: HttpRequestTestCase =>
            if (req.getProtocol == ShapeId.from("aws.protocols#restJson1"))
              req.toBuilder.protocol(ShapeId.from("alloy#simpleRestJson")).build()
            else req
        }.asJava)
        case c: HttpResponseTestsTrait => new HttpResponseTestsTrait(c.getSourceLocation, c.getTestCases.asScala.toList.map {
          case  res: HttpResponseTestCase =>
            if (res.getProtocol == ShapeId.from("aws.protocols#restJson1"))
              res.toBuilder.protocol(ShapeId.from("alloy#simpleRestJson")).build()
              else res
        }.asJava)
        case _ => theTrait
      }
    }
    ctx.getTransformer().mapTraits(ctx.getModel(), traitMapper)
  }

}
