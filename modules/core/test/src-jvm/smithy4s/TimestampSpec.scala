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

import cats.Show
import org.scalacheck.Gen.Choose
import org.scalacheck._
import smithy.api.TimestampFormat
import java.time._
import org.scalacheck.Prop._

class TimestampSpec() extends munit.FunSuite with munit.ScalaCheckSuite {

  private implicit val arbInstant: Arbitrary[Instant] = {
    implicit val c: Choose[Instant] =
      Choose.xmap[Long, Instant](
        x =>
          Instant.ofEpochSecond(
            x % 2000000000, {
              x & 3 match {
                case 0 => 0
                case 1 => x % 1000 * 1000000
                case 2 => x % 1000000 * 1000
                case _ => x % 1000000000
              }
            }
          ),
        x => x.getEpochSecond
      )
    Arbitrary(
      Gen.choose[Instant](Instant.MIN, Instant.MAX)
    )
  }

  private implicit val showInstant: Show[Instant] = Show.fromToString

  property("Converts from/to Instant") {
    forAll { (i: Instant) =>
      val ts = Timestamp.fromInstant(i)
      expect.same(ts.toInstant, i)
    }
  }

  property("Converts from/to OffsetDateTime") {
    forAll { (i: Instant) =>
      val odt = OffsetDateTime.ofInstant(i, ZoneOffset.UTC)
      val ts = Timestamp.fromOffsetDateTime(odt)
      expect.same(ts.toOffsetDateTime, odt)
    }
  }

  property("Converts from/to LocalDate") {
    forAll { (i: Instant) =>
      val ld = toLocalDate(i)
      val ts = Timestamp.fromLocalDate(ld)
      expect.same(ts.toLocalDate, ld)
    }
  }

  property("Converts to/from DATE_TIME format") {
    forAll { (i: Instant) =>
      val ts = Timestamp.fromInstant(i)
      val formatted = ts.format(TimestampFormat.DATE_TIME)
      val parsed = Timestamp.parse(formatted, TimestampFormat.DATE_TIME)
      expect.same(formatted, i.toString)
      expect.same(parsed, Some(ts))
    }
  }

  property("Converts to/from EPOCH_SECONDS format") {
    forAll { (i: Instant) =>
      val ts = Timestamp.fromInstant(i)
      val formatted = ts.format(TimestampFormat.EPOCH_SECONDS)
      val parsed = Timestamp.parse(formatted, TimestampFormat.EPOCH_SECONDS)
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

  private def toLocalDate(i: Instant): LocalDate =
    LocalDate.ofEpochDay(i.getEpochSecond / (24 * 60 * 60))
}
