package smithy4s.example

package object accept {
  type AcceptHeaderTestService[F[_]] = smithy4s.kinds.FunctorAlgebra[AcceptHeaderTestServiceGen, F]
  val AcceptHeaderTestService = AcceptHeaderTestServiceGen

  /** JSON payload type */
  type JsonPayload = smithy4s.example.accept.JsonPayload.Type
  /** Plain text payload type */
  type PlainTextPayload = smithy4s.example.accept.PlainTextPayload.Type
  /** PNG image blob type */
  type PngImage = smithy4s.example.accept.PngImage.Type
  /** XML payload type */
  type XmlPayload = smithy4s.example.accept.XmlPayload.Type

}