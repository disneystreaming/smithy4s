package smithy4s
package http.json

import smithy.api.JsonName
import smithy.api.TimestampFormat
import smithy4s.api.Discriminated
import smithy4s.internals.InputOutput
import smithy4s.internals.DiscriminatedUnionMember

final case class codecs(hintMask: HintMask = codecs.defaultHintMask)
    extends JsonCodecAPI(HintMask.mask(codecs.schematicJCodec, hintMask))

object codecs {

  val defaultHintMask: HintMask =
    HintMask(
      JsonName,
      TimestampFormat,
      Discriminated,
      InputOutput,
      Discriminated,
      DiscriminatedUnionMember
    )

  private[smithy4s] val schematicJCodec: Schematic[JCodec.JCodecMake] =
    new SchematicJCodec(Constraints.defaultConstraints, maxArity = 1024)

}
