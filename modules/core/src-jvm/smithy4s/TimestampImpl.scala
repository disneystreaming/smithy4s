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

import smithy.api.TimestampFormat.DATE_TIME
import smithy.api.TimestampFormat.EPOCH_SECONDS
import smithy.api.TimestampFormat.HTTP_DATE

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private[smithy4s] final class TimestampImpl private[smithy4s] (
    val dateTime: OffsetDateTime
) extends Timestamp {

  def year = dateTime.getYear()
  def month = dateTime.getMonthValue()
  def day = dateTime.getDayOfMonth()
  def hour = dateTime.getHour()
  def minute = dateTime.getMinute()
  def second = dateTime.getSecond()
  def nanoseconds = dateTime.getNano()
  def epochSecond: Long = dateTime.toEpochSecond()

  override def equals(obj: Any): Boolean =
    if (obj.isInstanceOf[TimestampImpl]) {
      this.dateTime.isEqual(obj.asInstanceOf[TimestampImpl].dateTime)
    } else false

  override def hashCode(): Int = dateTime.hashCode()

  def format(timestampFormat: smithy.api.TimestampFormat): String =
    timestampFormat match {
      case DATE_TIME =>
        dateTime.toInstant.toString
      case EPOCH_SECONDS =>
        val sb = new StringBuilder()
        sb.append(dateTime.toInstant().getEpochSecond())
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
      case HTTP_DATE =>
        dateTime.format(TimestampImpl.imfDateFormatter)
    }

  def toOffsetDateTime: OffsetDateTime = dateTime
  def toInstant: Instant = dateTime.toInstant()
  def toLocalDate: LocalDate = dateTime.toLocalDate()

}

private[smithy4s] object TimestampImpl {

  // private[smithy4s] val offsetDateTimeFormatter =
  //   DateTimeFormatter.ISO_OFFSET_DATE_TIME

  // https://datatracker.ietf.org/doc/html/rfc7231.html#section-7.1.1.1
  // Used to format timestamps that are annotated with timestampFormat("http-date").
  private[smithy4s] val imfDateFormatter = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
    .withZone(ZoneId.of("GMT"))

}
