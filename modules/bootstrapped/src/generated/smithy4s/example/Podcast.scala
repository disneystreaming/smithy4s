package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.union

sealed abstract class Podcast extends PodcastCommon with scala.Product with scala.Serializable {
  @inline final def widen: Podcast = this
  def _ordinal: Int
}
object Podcast extends ShapeTag.Companion[Podcast] {
  val id: ShapeId = ShapeId("smithy4s.example", "Podcast")

  val hints: Hints = Hints.empty

  final case class Video(title: Option[String] = None, url: Option[String] = None, durationMillis: Option[Long] = None) extends Podcast {
    def _ordinal: Int = 0
  }
  object Video extends ShapeTag.Companion[Video] {
    val id: ShapeId = ShapeId("smithy4s.example", "Video")

    val hints: Hints = Hints.empty

    val schema: Schema[Video] = struct(
      string.optional[Video]("title", _.title),
      string.optional[Video]("url", _.url),
      long.optional[Video]("durationMillis", _.durationMillis),
    ){
      Video.apply
    }.withId(id).addHints(hints)

    val alt = schema.oneOf[Podcast]("video")
  }
  final case class Audio(title: Option[String] = None, url: Option[String] = None, durationMillis: Option[Long] = None) extends Podcast {
    def _ordinal: Int = 1
  }
  object Audio extends ShapeTag.Companion[Audio] {
    val id: ShapeId = ShapeId("smithy4s.example", "Audio")

    val hints: Hints = Hints.empty

    val schema: Schema[Audio] = struct(
      string.optional[Audio]("title", _.title),
      string.optional[Audio]("url", _.url),
      long.optional[Audio]("durationMillis", _.durationMillis),
    ){
      Audio.apply
    }.withId(id).addHints(hints)

    val alt = schema.oneOf[Podcast]("audio")
  }


  implicit val schema: Schema[Podcast] = union(
    Video.alt,
    Audio.alt,
  ){
    _._ordinal
  }.withId(id).addHints(hints)
}
