package smithy4s

import java.time.Instant
import java.time.OffsetDateTime

private[smithy4s] trait TimestampCompanionPlatform {

  def nowUTC(): Timestamp = {
    val currentMillis = System.currentTimeMillis
    Timestamp(
      (currentMillis / 1000).toLong,
      (currentMillis % 1000).toInt * 100000
    )
  }

}
