package smithy4s

import java.time.Instant
import java.time.ZoneOffset
import java.time.OffsetDateTime

private[smithy4s] trait TimestampPlatform { self: Timestamp =>

  /** JVM platform only method */
  def toInstant: Instant = Instant.ofEpochSecond(epochSecond, nano.toLong)

  /** JVM platform only method */
  def toOffsetDateTime: OffsetDateTime =
    OffsetDateTime.ofInstant(
      Instant.ofEpochSecond(epochSecond, nano.toLong),
      ZoneOffset.UTC
    )

}
