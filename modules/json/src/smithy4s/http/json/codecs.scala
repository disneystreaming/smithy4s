package smithy4s
package http.json

import smithy4s.syntax._

final case class codecs(mask: HintMask)
    extends JsonCodecAPI(
      codecs.schematicJCodec
        .mask(mask)
    )

object codecs {
  val schematicJCodec =
    new SchematicJCodec(Constraints.defaultConstraints, maxArity = 1024)
}
