package smithy4s.example

import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
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

    val title: FieldLens[Video, Option[String]] = string.optional[Video]("title", _.title, n => c => c.copy(title = n))
    val url: FieldLens[Video, Option[String]] = string.optional[Video]("url", _.url, n => c => c.copy(url = n))
    val durationMillis: FieldLens[Video, Option[Long]] = long.optional[Video]("durationMillis", _.durationMillis, n => c => c.copy(durationMillis = n))

    val schema: Schema[Video] = struct(
      title,
      url,
      durationMillis,
    ){
      Video.apply
    }
    .withId(ShapeId("smithy4s.example", "Video"))
  }
  final case class Audio(title: Option[String] = None, url: Option[String] = None, durationMillis: Option[Long] = None) extends Podcast {
    def _ordinal: Int = 1
  }
  object Audio extends ShapeTag.Companion[Audio] {

    val title: FieldLens[Audio, Option[String]] = string.optional[Audio]("title", _.title, n => c => c.copy(title = n))
    val url: FieldLens[Audio, Option[String]] = string.optional[Audio]("url", _.url, n => c => c.copy(url = n))
    val durationMillis: FieldLens[Audio, Option[Long]] = long.optional[Audio]("durationMillis", _.durationMillis, n => c => c.copy(durationMillis = n))

    val schema: Schema[Audio] = struct(
      title,
      url,
      durationMillis,
    ){
      Audio.apply
    }
    .withId(ShapeId("smithy4s.example", "Audio"))
  }


  val video = Video.schema.oneOf[Podcast]("video")
  val audio = Audio.schema.oneOf[Podcast]("audio")

  implicit val schema: Schema[Podcast] = union(
    video,
    audio,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "Podcast"))
}
