package smithy4s

import java.time.Instant
import java.time.OffsetDateTime

private[smithy4s] trait TimestampCompanionPlatform {

  /** JVM platform only method */
  def fromInstant(x: Instant): Timestamp =
    Timestamp(x.getEpochSecond, x.getNano)

  /** JVM platform only method */
  def fromOffsetDateTime(x: OffsetDateTime): Timestamp =
    Timestamp(x.toInstant.getEpochSecond, x.getNano)

  def nowUTC(): Timestamp = fromInstant(Instant.now())

}
