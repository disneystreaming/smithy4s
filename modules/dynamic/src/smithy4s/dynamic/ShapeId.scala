package smithy4s.dynamic

case class ShapeId(namespace: String, name: String) {
  def withMember(member: String): ShapeId.Member = ShapeId.Member(this, member)
}

object ShapeId {

  case class Member(shapeId: ShapeId, member: String)

  def fromParts(namespace: String, name: String) = ShapeId(namespace, name)

}
