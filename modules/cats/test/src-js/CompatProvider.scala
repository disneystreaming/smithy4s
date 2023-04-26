package smithy4s.interopcats

import scalajs.js.Date
import smithy4s.Timestamp


trait CompatProvider {
    def getTimestamp: Timestamp = {
      val d = new Date()
     Timestamp.fromDate(d)
    }
}
