package smithy.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait HttpMalformedResponseBodyAssertion extends scala.Product with scala.Serializable {
  @inline final def widen: HttpMalformedResponseBodyAssertion = this
}
object HttpMalformedResponseBodyAssertion extends ShapeTag.Companion[HttpMalformedResponseBodyAssertion] {
  val id: ShapeId = ShapeId("smithy.test", "HttpMalformedResponseBodyAssertion")

  val hints: Hints = Hints(
    smithy.api.Private(),
  )

  /** Defines the expected serialized response body, which will be matched
    * exactly.
    */
  final case class ContentsCase(contents: String) extends HttpMalformedResponseBodyAssertion
  /** A regex to evaluate against the `message` field in the body. For
    * responses that may have some variance from platform to platform,
    * such as those that include messages from a parser.
    */
  final case class MessageRegexCase(messageRegex: String) extends HttpMalformedResponseBodyAssertion

  object ContentsCase {
    val hints: Hints = Hints(
      smithy.api.Documentation("Defines the expected serialized response body, which will be matched\nexactly."),
    )
    val schema: Schema[ContentsCase] = bijection(string.addHints(hints), ContentsCase(_), _.contents)
    val alt = schema.oneOf[HttpMalformedResponseBodyAssertion]("contents")
  }
  object MessageRegexCase {
    val hints: Hints = Hints(
      smithy.api.Documentation("A regex to evaluate against the `message` field in the body. For\nresponses that may have some variance from platform to platform,\nsuch as those that include messages from a parser."),
    )
    val schema: Schema[MessageRegexCase] = bijection(string.addHints(hints), MessageRegexCase(_), _.messageRegex)
    val alt = schema.oneOf[HttpMalformedResponseBodyAssertion]("messageRegex")
  }

  implicit val schema: Schema[HttpMalformedResponseBodyAssertion] = union(
    ContentsCase.alt,
    MessageRegexCase.alt,
  ){
    case c: ContentsCase => ContentsCase.alt(c)
    case c: MessageRegexCase => MessageRegexCase.alt(c)
  }.withId(id).addHints(hints)
}
