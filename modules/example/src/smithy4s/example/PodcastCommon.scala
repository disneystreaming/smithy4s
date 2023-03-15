package smithy4s.example


trait PodcastCommon {
  def title: Option[String]
  def url: Option[String]
  def durationMillis: Option[Long]
}