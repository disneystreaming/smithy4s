package smithy4s.xml
import smithy4s.xml.XPath.Segment.Index
import smithy4s.xml.XPath.Segment.Tag
import smithy4s.xml.XPath.Segment.Attr

case class XPath(reversedSegments: List[XPath.Segment]) {
  def render: String = reversedSegments.reverse.map(_.render).mkString(".")

  def appendIndex(index: Int): XPath = XPath(
    XPath.Segment.Index(index) :: reversedSegments
  )
  def appendTag(tag: String): XPath = XPath(
    XPath.Segment.Tag(tag) :: reversedSegments
  )
  def appendAttr(name: String): XPath = XPath(
    XPath.Segment.Attr(name) :: reversedSegments
  )
}

object XPath {
  val root = XPath(List.empty)

  def attr(name: String): XPath = XPath(List(XPath.Segment.Attr(name)))

  sealed trait Segment {
    def render: String = this match {
      case Index(index) => s"[index:$index]"
      case Tag(tag)     => tag
      case Attr(attr)   => s"attr:$attr"
    }
  }
  object Segment {
    case class Index(index: Int) extends Segment
    case class Tag(tag: String) extends Segment
    case class Attr(attr: String) extends Segment
  }

}
