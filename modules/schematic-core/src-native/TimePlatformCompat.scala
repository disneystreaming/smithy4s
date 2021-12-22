package schematic

import scala.scalanative.unsafe._
import scala.scalanative.posix
import posix.time
import posix.timeOps._
import java.time.{OffsetDateTime, ZoneId}

// See https://www.epochconverter.com/programming/c
private[schematic] final case class TimestampImpl(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    second: Int
) extends Timestamp {
  def epochSecond: Long = Zone { implicit zone =>
    val t = alloc[time.tm]
    t.tm_year = year - 1900 // Year - 1900
    t.tm_mon = month - 1 // Month, where 0 = jan
    t.tm_mday = day // Day of the month
    t.tm_hour = hour
    t.tm_min = minute
    t.tm_sec = second
    t.tm_isdst = -1; // Is DST on? 1 = yes, 0 = no, -1 = unknown
    time.mktime(t).longValue()
  }
}

trait TimePlatformCompat extends TimestampCompanion {
  def apply(
      year: Int,
      month: Int,
      day: Int,
      hour: Int,
      minute: Int,
      second: Int
  ): Timestamp =
    TimestampImpl(year, month, day, hour, minute, second)

  def nowUTC(): Timestamp = Zone { implicit zone =>
    val out = alloc[time.tm]
    val timePtr = alloc[time.time_t]
    !timePtr = time.time(null)
    val gmTime: Ptr[time.tm] = time.gmtime_r(timePtr, out)
    Timestamp(
      gmTime.tm_year + 1900,
      gmTime.tm_mon + 1,
      gmTime.tm_mday,
      gmTime.tm_hour,
      gmTime.tm_min,
      gmTime.tm_sec
    )
  }

  def fromEpochSecond(epochSecond: Long): Timestamp = Zone { implicit zone =>
    val out = alloc[time.tm]
    val timePtr = alloc[time.time_t]
    !timePtr = epochSecond
    val gmTime: Ptr[time.tm] = time.localtime_r(timePtr, out)
    Timestamp(
      gmTime.tm_year + 1900,
      gmTime.tm_mon + 1,
      gmTime.tm_mday,
      gmTime.tm_hour,
      gmTime.tm_min,
      gmTime.tm_sec
    )
  }
}
