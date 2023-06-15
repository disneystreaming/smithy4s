package aws.api

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

/** Designates the target as containing data of a known classification level.
  * @param CUSTOMER_CONTENT
  *   Customer content means any software (including machine images), data,
  *   text, audio, video or images that customers or any customer end user
  *   transfers to AWS for processing, storage or hosting by AWS services in
  *   connection with the customer’s accounts and any computational results
  *   that customers or any customer end user derive from the foregoing
  *   through their use of AWS services.
  * @param CUSTOMER_ACCOUNT_INFORMATION
  *   Account information means information about customers that customers
  *   provide to AWS in connection with the creation or administration of
  *   customers’ accounts.
  * @param TAG_DATA
  *   Designates metadata tags applied to AWS resources.
  * @param PERMISSIONS_DATA
  *   Designates security and access roles, rules, usage policies, and
  *   permissions.
  * @param SERVICE_ATTRIBUTES
  *   Service Attributes means service usage data related to a customer’s
  *   account, such as resource identifiers, metadata tags, security and
  *   access roles, rules, usage policies, permissions, usage statistics,
  *   logging data, and analytics.
  */
sealed abstract class Data(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = Data
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = Data
  @inline final def widen: Data = this
}
object Data extends Enumeration[Data] with ShapeTag.Companion[Data] {
  val id: ShapeId = ShapeId("aws.api", "data")

  val hints: Hints = Hints(
    smithy.api.Documentation("Designates the target as containing data of a known classification level."),
    smithy.api.Trait(selector = Some(":test(simpleType, list, structure, union, member)"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  /** Customer content means any software (including machine images), data,
    * text, audio, video or images that customers or any customer end user
    * transfers to AWS for processing, storage or hosting by AWS services in
    * connection with the customer’s accounts and any computational results
    * that customers or any customer end user derive from the foregoing
    * through their use of AWS services.
    */
  case object CUSTOMER_CONTENT extends Data("content", "CUSTOMER_CONTENT", 0, Hints(smithy.api.Documentation("Customer content means any software (including machine images), data,\ntext, audio, video or images that customers or any customer end user\ntransfers to AWS for processing, storage or hosting by AWS services in\nconnection with the customer’s accounts and any computational results\nthat customers or any customer end user derive from the foregoing\nthrough their use of AWS services.")))
  /** Account information means information about customers that customers
    * provide to AWS in connection with the creation or administration of
    * customers’ accounts.
    */
  case object CUSTOMER_ACCOUNT_INFORMATION extends Data("account", "CUSTOMER_ACCOUNT_INFORMATION", 1, Hints(smithy.api.Documentation("Account information means information about customers that customers\nprovide to AWS in connection with the creation or administration of\ncustomers’ accounts.")))
  /** Service Attributes means service usage data related to a customer’s
    * account, such as resource identifiers, metadata tags, security and
    * access roles, rules, usage policies, permissions, usage statistics,
    * logging data, and analytics.
    */
  case object SERVICE_ATTRIBUTES extends Data("usage", "SERVICE_ATTRIBUTES", 2, Hints(smithy.api.Documentation("Service Attributes means service usage data related to a customer’s\naccount, such as resource identifiers, metadata tags, security and\naccess roles, rules, usage policies, permissions, usage statistics,\nlogging data, and analytics.")))
  /** Designates metadata tags applied to AWS resources. */
  case object TAG_DATA extends Data("tagging", "TAG_DATA", 3, Hints(smithy.api.Documentation("Designates metadata tags applied to AWS resources.")))
  /** Designates security and access roles, rules, usage policies, and
    * permissions.
    */
  case object PERMISSIONS_DATA extends Data("permissions", "PERMISSIONS_DATA", 4, Hints(smithy.api.Documentation("Designates security and access roles, rules, usage policies, and\npermissions.")))

  val values: List[Data] = List(
    CUSTOMER_CONTENT,
    CUSTOMER_ACCOUNT_INFORMATION,
    SERVICE_ATTRIBUTES,
    TAG_DATA,
    PERMISSIONS_DATA,
  )
  val tag: EnumTag = EnumTag.StringEnum
  implicit val schema: Schema[Data] = enumeration(tag, values).withId(id).addHints(hints)
}
