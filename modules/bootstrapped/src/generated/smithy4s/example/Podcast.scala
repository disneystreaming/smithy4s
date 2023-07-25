package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.optics.Prism
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.union

sealed trait Podcast extends PodcastCommon with scala.Product with scala.Serializable {
  @inline final def widen: Podcast = this
  def _ordinal: Int
}
object Podcast extends ShapeTag.Companion[Podcast] {
  val id: ShapeId = ShapeId("smithy4s.example", "Podcast")

  val hints: Hints = Hints.empty

  object Optics {
    val video: Prism[Podcast, Video] = Prism.partial[Podcast, Video]{ case t: Video => t }(identity)
    val audio: Prism[Podcast, Audio] = Prism.partial[Podcast, Audio]{ case t: Audio => t }(identity)
  }

  final case class Video(title: Option[String] = None, url: Option[String] = None, durationMillis: Option[Long] = None) extends Podcast {
    def _ordinal: Int = 0
  }
  object Video extends ShapeTag.Companion[Video] {
    val id: ShapeId = ShapeId("smithy4s.example", "Video")

    val hints: Hints = Hints.empty

    object Optics {
      val title: Lens[Video, Option[String]] = Lens[Video, Option[String]](_.title)(n => a => a.copy(title = n))
      val url: Lens[Video, Option[String]] = Lens[Video, Option[String]](_.url)(n => a => a.copy(url = n))
      val durationMillis: Lens[Video, Option[Long]] = Lens[Video, Option[Long]](_.durationMillis)(n => a => a.copy(durationMillis = n))
    }

    val title = string.optional[Video]("title", _.title)
    val url = string.optional[Video]("url", _.url)
    val durationMillis = long.optional[Video]("durationMillis", _.durationMillis)

    val schema: Schema[Video] = struct(
      title,
      url,
      durationMillis,
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

    object Optics {
      val title: Lens[Audio, Option[String]] = Lens[Audio, Option[String]](_.title)(n => a => a.copy(title = n))
      val url: Lens[Audio, Option[String]] = Lens[Audio, Option[String]](_.url)(n => a => a.copy(url = n))
      val durationMillis: Lens[Audio, Option[Long]] = Lens[Audio, Option[Long]](_.durationMillis)(n => a => a.copy(durationMillis = n))
    }

    val title = string.optional[Audio]("title", _.title)
    val url = string.optional[Audio]("url", _.url)
    val durationMillis = long.optional[Audio]("durationMillis", _.durationMillis)

    val schema: Schema[Audio] = struct(
      title,
      url,
      durationMillis,
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
