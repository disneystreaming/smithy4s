package smithy4s.xml.internals

import smithy4s.schema.Schema
import Schema._
import smithy4s.ShapeTag
import smithy4s.ShapeId

case class XmlStartingPath(path: List[String])

object XmlStartingPath extends ShapeTag.Companion[XmlStartingPath] {

  val id: ShapeId = ShapeId("smithy4s.xml.internals", "XmlStartingPath")

  val schema: Schema[XmlStartingPath] =
    list(string)
      .biject[XmlStartingPath](
        XmlStartingPath(_),
        (_: XmlStartingPath).path
      )

}
