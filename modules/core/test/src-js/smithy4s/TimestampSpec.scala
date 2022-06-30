/*
 *  Copyright 2021 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s

import org.scalacheck.Gen.Choose
import org.scalacheck._
import smithy.api.TimestampFormat
import scalajs.js.Date
import org.scalacheck.Prop._

class TimestampSpec() extends munit.FunSuite with munit.ScalaCheckSuite {

  private implicit val arbDate: Arbitrary[Date] = {
    implicit val c: Choose[Date] =
      Choose.xmap[Long, Date](
        x => {
          // The 0 there is the key, which sets the date to the epoch
          val date = new Date(0)
          date.setUTCSeconds(
            (x % 2000000000).toDouble + ((x % 1000).toDouble / 1000)
          )
          date
        },
        x => (x.valueOf() / 1000).toLong
      )
    Arbitrary(
      Gen.choose[Date](
        {
          // The 0 there is the key, which sets the date to the epoch
          val date = new Date(0)
          date.setUTCSeconds(-6.21672192e10)
          date
        }, {
          // The 0 there is the key, which sets the date to the epoch
          val date = new Date(0)
          date.setUTCSeconds(2.53402300799e11)
          date
        }
      )
    )
  }

  property("Converts from/to Date") {
    forAll { (i: Date) =>
      val epochSecond = (i.valueOf() / 1000).toLong
      val nano = (i.valueOf() % 1000).toInt * 1000000
      val ts = Timestamp(epochSecond, nano)
      val d = ts.toDate
      val ts2 = Timestamp.fromDate(d)
      expect.same(d.toString, i.toString)
      expect.same(ts, ts2)
    }
  }

  property("Converts to/from DATE_TIME format") {
    forAll { (i: Date) =>
      val epochSecond = (i.valueOf() / 1000).toLong
      val nano = (i.valueOf() % 1000).toInt * 1000000
      val ts = Timestamp(epochSecond, nano)
      val formatted = ts.format(TimestampFormat.DATE_TIME)
      val parsed = Timestamp.parse(formatted, TimestampFormat.DATE_TIME)
      expect.same(formatted, i.toISOString().replace(".000", ""))
      expect.same(parsed, Some(ts))
    }
  }

  property("Converts to/from HTTP_DATE format") {
    forAll { (i: Date) =>
      val epochSecond = (i.valueOf() / 1000).toLong
      val nano = (i.valueOf() % 1000).toInt * 1000000
      val ts = Timestamp(epochSecond, nano)
      val formatted = ts.format(TimestampFormat.HTTP_DATE)
      val parsed = Timestamp.parse(formatted, TimestampFormat.HTTP_DATE)
      expect.same(formatted, i.toUTCString())
      expect.same(parsed, Some(ts))
    }
  }

  property("Converts to/from EPOCH_SECONDS format") {
    forAll { (i: Date) =>
      val epochSecond = (i.valueOf() / 1000).toLong
      val nano = (i.valueOf() % 1000).toInt * 1000000
      val ts = Timestamp(epochSecond, nano)
      val formatted = ts.format(TimestampFormat.EPOCH_SECONDS)
      val parsed = Timestamp.parse(formatted, TimestampFormat.EPOCH_SECONDS)
      val expected =
        if (nano != 0) {
          var s = s"$epochSecond.${nano + 1000000000}".replace(".1", ".")
          if (s.endsWith("000")) s = s.substring(0, s.length - 3)
          if (s.endsWith("000")) s = s.substring(0, s.length - 3)
          s
        } else epochSecond.toString
      expect.same(formatted, expected)
      expect.same(parsed, Some(ts))
    }
  }

  property("Parse EPOCH_SECONDS format with invalid input") {
    val EpochFormat = """^(\d+)(\.(\d+))?""".r
    forAll { (str: String) =>
      val parsed = Timestamp.parse(str, TimestampFormat.EPOCH_SECONDS)
      parsed match {
        case Some(_) => expect(EpochFormat.pattern.matcher(str).matches)
        case None    => expect(!EpochFormat.pattern.matcher(str).matches)
      }
    }
  }

  property("Parse EPOCH_SECONDS format with too many decimals") {
    forAll { (i: Int) =>
      val str = s"$i.${i % 1000}.${i % 1000}"
      val parsed = Timestamp.parse(str, TimestampFormat.EPOCH_SECONDS)
      expect.same(parsed, None)
    }
  }
}
