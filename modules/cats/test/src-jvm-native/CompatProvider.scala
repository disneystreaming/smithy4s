package smithy4s.interopcats

import smithy4s.Timestamp

trait CompatProvider {

  def getTimestamp: Timestamp = {
    val now = java.time.Instant.now()
    Timestamp.fromEpochSecond(now.getEpochSecond)
  }
}
