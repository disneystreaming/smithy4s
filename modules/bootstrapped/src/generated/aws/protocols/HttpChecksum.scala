package aws.protocols

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** Indicates that an operation supports checksum validation.
  * @param requestAlgorithmMember
  *   Defines a top-level operation input member that is used to configure
  *   request checksum behavior.
  * @param requestChecksumRequired
  *   Indicates an operation requires a checksum in its HTTP request.
  * @param requestValidationModeMember
  *   Defines a top-level operation input member used to opt-in to response
  *   checksum validation.
  * @param responseAlgorithms
  *   Defines the checksum algorithms clients should look for when performing
  *   HTTP response checksum validation.
  */
final case class HttpChecksum(requestAlgorithmMember: Option[String] = None, requestChecksumRequired: Option[Boolean] = None, requestValidationModeMember: Option[String] = None, responseAlgorithms: Option[Set[ChecksumAlgorithm]] = None)
object HttpChecksum extends ShapeTag.Companion[HttpChecksum] {
  val id: ShapeId = ShapeId("aws.protocols", "httpChecksum")

  val hints: Hints = Hints(
    smithy.api.Documentation("Indicates that an operation supports checksum validation."),
    smithy.api.Unstable(),
    smithy.api.Trait(selector = Some("operation"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[HttpChecksum] = recursive(struct(
    string.optional[HttpChecksum]("requestAlgorithmMember", _.requestAlgorithmMember).addHints(smithy.api.Documentation("Defines a top-level operation input member that is used to configure\nrequest checksum behavior.")),
    boolean.optional[HttpChecksum]("requestChecksumRequired", _.requestChecksumRequired).addHints(smithy.api.Documentation("Indicates an operation requires a checksum in its HTTP request.")),
    string.optional[HttpChecksum]("requestValidationModeMember", _.requestValidationModeMember).addHints(smithy.api.Documentation("Defines a top-level operation input member used to opt-in to response\nchecksum validation.")),
    ChecksumAlgorithmSet.underlyingSchema.optional[HttpChecksum]("responseAlgorithms", _.responseAlgorithms).addHints(smithy.api.Documentation("Defines the checksum algorithms clients should look for when performing\nHTTP response checksum validation.")),
  ){
    HttpChecksum.apply
  }.withId(id).addHints(hints))
}
