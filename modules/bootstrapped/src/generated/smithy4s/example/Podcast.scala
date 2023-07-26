package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.union

sealed trait Podcast extends PodcastCommon with scala.Product with scala.Serializable {
  @inline final def widen: Podcast = this
  def _ordinal: Int
}
object Podcast extends ShapeTag.Companion[Podcast] {
  final case class Video(title: Option[String] = None, url: Option[String] = None, durationMillis: Option[Long] = None) extends Podcast {
    def _ordinal: Int = 0
  }
  object Video extends ShapeTag.Companion[Video] {

    val title = string.optional[Video]("title", _.title, n => c => c.copy(title = n))
    val url = string.optional[Video]("url", _.url, n => c => c.copy(url = n))
    val durationMillis = long.optional[Video]("durationMillis", _.durationMillis, n => c => c.copy(durationMillis = n))

    val schema: Schema[Video] = struct(
      title,
      url,
      durationMillis,
    ){
      Video.apply
    }
    .withId(ShapeId("smithy4s.example", "Video"))
    .addHints(
      Hints.empty
    )

    val alt = schema.oneOf[Podcast]("video")
  }
  final case class Audio(title: Option[String] = None, url: Option[String] = None, durationMillis: Option[Long] = None) extends Podcast {
    def _ordinal: Int = 1
  }
  object Audio extends ShapeTag.Companion[Audio] {

    val title = string.optional[Audio]("title", _.title, n => c => c.copy(title = n))
    val url = string.optional[Audio]("url", _.url, n => c => c.copy(url = n))
    val durationMillis = long.optional[Audio]("durationMillis", _.durationMillis, n => c => c.copy(durationMillis = n))

    val schema: Schema[Audio] = struct(
      title,
      url,
      durationMillis,
    ){
      Audio.apply
    }
    .withId(ShapeId("smithy4s.example", "Audio"))
    .addHints(
      Hints.empty
    )

    val alt = schema.oneOf[Podcast]("audio")
  }


  implicit val schema: Schema[Podcast] = union(
    Video.alt,
    Audio.alt,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "Podcast"))
  .addHints(
    Hints.empty
  )
}
