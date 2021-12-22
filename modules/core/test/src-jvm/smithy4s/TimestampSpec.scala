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
import weaver._
import weaver.scalacheck._

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

object TimestampSpec extends SimpleIOSuite with Checkers {

  private implicit val arbInstant: Arbitrary[Instant] = {

    implicit val c: Choose[Instant] =
      Choose.xmap[Long, Instant](Instant.ofEpochSecond(_), _.getEpochSecond)

    Arbitrary(
      Gen.choose[Instant](Instant.MIN, Instant.MAX)
    )
  }

  private implicit val showInstant: Show[Instant] = Show.fromToString

  pureTest("Converts from/to offsetdatetime") {
    val ts = Timestamp(1988, 4, 11, 13, 24, 32)
    val odt = OffsetDateTime.of(1988, 4, 11, 13, 24, 32, 0, ZoneOffset.UTC)
    val ts2 = Timestamp.fromOffsetDateTime(odt)
    val ts3 = Timestamp.fromOffsetDateTime(
      odt.withOffsetSameInstant(ZoneOffset.ofHours(1))
    )
    expect.same(ts.toOffsetDateTime, odt) &&
    expect.same(ts, ts2) &&
    expect.same(ts, ts3)
  }

  pureTest("Converts from/to instant") {
    val ts = Timestamp(1988, 4, 11, 13, 24, 32)
    val epochSecond = 576768272L
    val instant = Instant.ofEpochSecond(epochSecond)
    val ts2 = Timestamp.fromInstant(instant)
    expect.same(ts.toInstant, instant) &&
    expect.same(ts, ts2) &&
    expect.same(ts.epochSecond, instant.getEpochSecond())
  }

  pureTest("Converts from/to local-date") {
    val ts = Timestamp(1988, 4, 11, 0, 0, 0)
    val localDate = LocalDate.of(1988, 4, 11)
    val ts2 = Timestamp.fromLocalDate(localDate)
    expect.same(ts.toLocalDate, localDate) &&
    expect.same(ts, ts2)
  }

  pureTest("Format: date-time") {
    val ts = Timestamp(1988, 4, 11, 13, 24, 32, 333000000)
    val formatted = ts.format(TimestampFormat.DATE_TIME)
    val roundTripped = Timestamp.parse(formatted, TimestampFormat.DATE_TIME)
    expect.same(formatted, "1988-04-11T13:24:32.333Z") &&
    expect.same(roundTripped, Some(ts))
  }

  pureTest("Format: epochSeconds") {
    val ts = Timestamp(1988, 4, 11, 13, 24, 32, 333000000)
    val formatted = ts.format(TimestampFormat.EPOCH_SECONDS)
    val roundTripped = Timestamp.parse(formatted, TimestampFormat.EPOCH_SECONDS)
    expect.same(formatted, "576768272.333") &&
    expect.same(roundTripped, Some(ts))
  }

  test("Format: epochSeconds valid input") {
    forall { (i: Instant) =>
      val roundTripped =
        Timestamp.parse(
          s"${i.getEpochSecond}.${i.getNano}",
          TimestampFormat.EPOCH_SECONDS
        )
      expect.same(roundTripped, Some(Timestamp.fromInstant(i)))
    }
  }

  val EpochFormat = """^(\d+)(\.(\d+))?""".r
  test("Format: epochSeconds invalid input") {
    forall { (str: String) =>
      val roundTripped =
        Timestamp.parse(
          str,
          TimestampFormat.EPOCH_SECONDS
        )
      val asst = expect(EpochFormat.pattern.matcher(str).matches)
      roundTripped match {
        case Some(_) => asst
        case None    => not(asst)
      }
    }
  }

  pureTest("Format: epochSeconds too many decimals") {
    val str = "1234.123.12"
    val roundTripped =
      Timestamp.parse(
        str,
        TimestampFormat.EPOCH_SECONDS
      )
    expect.same(roundTripped, None)
  }
}
