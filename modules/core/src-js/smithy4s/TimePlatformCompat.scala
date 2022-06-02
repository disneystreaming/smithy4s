/*
 *  Copyright 2021-2022 Disney Streaming
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

import smithy.api.TimestampFormat

import scalajs.js.Date

private[smithy4s] final case class TimestampImpl(date: Date) extends Timestamp {
  def year = date.getUTCFullYear().toInt
  def month = date.getUTCMonth().toInt + 1
  def day = date.getUTCDate().toInt
  def hour = date.getUTCHours().toInt
  def minute = date.getUTCMinutes().toInt
  def second = date.getUTCSeconds().toInt
  def nanoseconds = date.getUTCMilliseconds().toInt * 1000000

  def format(timestampFormat: TimestampFormat): String =
    timestampFormat match {
      case TimestampFormat.DATE_TIME =>
        date.toISOString()
      case TimestampFormat.EPOCH_SECONDS =>
        val sb = new StringBuilder()
        sb.append(epochSecond)
        var decimal = nanoseconds
        if (decimal != 0) {
          sb.append('.')
          var i = 0
          while ((i <= 8) && (decimal != 0)) {
            val tens = math.pow(10, (8 - i).toDouble).toInt
            val digit = decimal / tens
            sb.append(digit)
            decimal -= (digit * tens)
            i += 1
          }
        }
        sb.result()
      case TimestampFormat.HTTP_DATE =>
        date.toUTCString()
    }

  def epochSecond: Long = (date.valueOf() / 1000).toLong
  override def equals(other: Any) =
    if (other == null) false
    else if (other.isInstanceOf[TimestampImpl]) {
      val otherT = other.asInstanceOf[TimestampImpl]
      epochSecond == otherT.epochSecond &&
      nanoseconds == otherT.nanoseconds
    } else false

  override def hashCode() = {
    epochSecond.hashCode + 31 * nanoseconds.hashCode
  }
}

private[smithy4s] trait TimePlatformCompat extends TimestampCompanion {

  def apply(
      year: Int,
      month: Int,
      day: Int,
      hour: Int,
      minute: Int,
      second: Int,
      nanoseconds: Int
  ): Timestamp = {
    val utc = Date.UTC(
      year,
      month - 1,
      day,
      hour,
      minute,
      second,
      nanoseconds / 1000000
    )
    TimestampImpl(new Date(utc))
  }

  def nowUTC(): Timestamp = {
    val date = new Date()
    val nowUtc = Date.UTC(
      date.getUTCFullYear().toInt,
      date.getUTCMonth().toInt,
      date.getUTCDate().toInt,
      date.getUTCHours().toInt,
      date.getUTCMinutes().toInt,
      date.getUTCSeconds().toInt
    )
    TimestampImpl(new Date(nowUtc))
  }

  def fromEpochSecond(epochSecond: Long): Timestamp = {
    // The 0 there is the key, which sets the date to the epoch
    val date = new Date(0)
    date.setUTCSeconds(epochSecond.toDouble)
    TimestampImpl(date)
  }

  def parseHttpDate(string: String): Option[smithy4s.Timestamp] = try {
    Some(TimestampImpl(new Date(Date.parse(string))))
  } catch {
    case _: Throwable => None
  }

  def parseDateTime(string: String): Option[smithy4s.Timestamp] = try {
    Some(TimestampImpl(new Date(Date.parse(string))))
  } catch {
    case _: Throwable => None
  }

}
