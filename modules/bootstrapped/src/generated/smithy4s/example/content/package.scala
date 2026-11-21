package smithy4s.example

package object content {
  type ContentHeaderTestService[F[_]] = smithy4s.kinds.FunctorAlgebra[ContentHeaderTestServiceGen, F]
  val ContentHeaderTestService = ContentHeaderTestServiceGen

  /** JSON payload type */
  type JsonPayload = smithy4s.example.content.JsonPayload.Type
  /** Plain text payload type */
  type PlainTextPayload = smithy4s.example.content.PlainTextPayload.Type
  /** PNG image blob type */
  type PngImage = smithy4s.example.content.PngImage.Type
  /** XML payload type */
  type XmlPayload = smithy4s.example.content.XmlPayload.Type

}