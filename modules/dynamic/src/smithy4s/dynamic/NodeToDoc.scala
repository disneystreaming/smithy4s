package smithy4s.dynamic

import smithy4s.Document
import software.amazon.smithy.model.node._
import scala.jdk.CollectionConverters._

object NodeToDoc {

  def apply(node: Node): Document =
    return node.accept(new NodeVisitor[Document] {
      def arrayNode(x: ArrayNode): Document =
        Document.array(x.getElements().asScala.map(_.accept(this)))

      def booleanNode(x: BooleanNode): Document =
        Document.fromBoolean(x.getValue())

      def nullNode(x: NullNode): Document =
        Document.DNull

      def numberNode(x: NumberNode): Document =
        Document.fromDouble(x.getValue().doubleValue())

      def objectNode(x: ObjectNode): Document =
        Document.obj(
          x.getMembers()
            .asScala
            .map { case (key, value) =>
              key.getValue() -> value.accept(this)
            }
            .toSeq: _*
        )

      def stringNode(x: StringNode): Document =
        Document.fromString(x.getValue())
    })

}
