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

sealed trait Podcast extends PodcastCommon with scala.Product with scala.Serializable { self =>
  @inline final def widen: Podcast = this
  def $ordinal: Int

  object project {
    def video: Option[Podcast.Video] = Podcast.Video.alt.project.lift(self)
    def audio: Option[Podcast.Audio] = Podcast.Audio.alt.project.lift(self)
  }

  def accept[A](visitor: Podcast.Visitor[A]): A = this match {
    case value: Podcast.Video => visitor.video(value)
    case value: Podcast.Audio => visitor.audio(value)
  }
}
object Podcast extends ShapeTag.Companion[Podcast] {

  def video(title: Option[String] = None, url: Option[String] = None, durationMillis: Option[Long] = None):Video = Video(title, url, durationMillis)
  def audio(title: Option[String] = None, url: Option[String] = None, durationMillis: Option[Long] = None):Audio = Audio(title, url, durationMillis)

  val id: ShapeId = ShapeId("smithy4s.example", "Podcast")

  val hints: Hints = Hints.empty

  object optics {
    val video: Prism[Podcast, Video] = Prism.partial[Podcast, Video]{ case t: Video => t }(identity)
    val audio: Prism[Podcast, Audio] = Prism.partial[Podcast, Audio]{ case t: Audio => t }(identity)
  }

  final case class Video(title: Option[String] = None, url: Option[String] = None, durationMillis: Option[Long] = None) extends Podcast {
    def $ordinal: Int = 0
  }

  object Video extends ShapeTag.Companion[Video] {
    val id: ShapeId = ShapeId("smithy4s.example", "Video")

    val hints: Hints = Hints.empty

    object optics {
      val title: Lens[Video, Option[String]] = Lens[Video, Option[String]](_.title)(n => a => a.copy(title = n))
      val url: Lens[Video, Option[String]] = Lens[Video, Option[String]](_.url)(n => a => a.copy(url = n))
      val durationMillis: Lens[Video, Option[Long]] = Lens[Video, Option[Long]](_.durationMillis)(n => a => a.copy(durationMillis = n))
    }

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
    def $ordinal: Int = 1
  }

  object Audio extends ShapeTag.Companion[Audio] {
    val id: ShapeId = ShapeId("smithy4s.example", "Audio")

    val hints: Hints = Hints.empty

    object optics {
      val title: Lens[Audio, Option[String]] = Lens[Audio, Option[String]](_.title)(n => a => a.copy(title = n))
      val url: Lens[Audio, Option[String]] = Lens[Audio, Option[String]](_.url)(n => a => a.copy(url = n))
      val durationMillis: Lens[Audio, Option[Long]] = Lens[Audio, Option[Long]](_.durationMillis)(n => a => a.copy(durationMillis = n))
    }

    val schema: Schema[Audio] = struct(
      string.optional[Audio]("title", _.title),
      string.optional[Audio]("url", _.url),
      long.optional[Audio]("durationMillis", _.durationMillis),
    ){
      Audio.apply
    }.withId(id).addHints(hints)

    val alt = schema.oneOf[Podcast]("audio")
  }


  trait Visitor[A] {
    def video(value: Podcast.Video): A
    def audio(value: Podcast.Audio): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def video(value: Podcast.Video): A = default
      def audio(value: Podcast.Audio): A = default
    }
  }

  implicit val schema: Schema[Podcast] = union(
    Podcast.Video.alt,
    Podcast.Audio.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
