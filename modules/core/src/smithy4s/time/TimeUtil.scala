/*
 *  Copyright 2021-2025 Disney Streaming
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

package smithy4s.time

private[time] object TimeUtil {
  def append4Digits(x: Int, s: java.lang.StringBuilder): Unit = {
    val q = x * 5243 >> 19 // divide a 4-digit positive int by 100
    append2Digits(q, s)
    append2Digits(x - q * 100, s)
  }

  def append3Digits(x: Int, s: java.lang.StringBuilder): Unit = {
    val q = x * 5243 >> 19 // divide a 4-digit positive int by 100
    s.append((q + '0').toChar)
    append2Digits(x - q * 100, s)
  }

  def append2Digits(x: Int, s: java.lang.StringBuilder): Unit = {
    val d = digits(x)
    val _ = s.append((d & 0xff).toChar).append((d >> 8).toChar)
  }

  def appendNano(nano: Int, s: java.lang.StringBuilder): Unit =
    if (nano != 0) {
      val y1 =
        nano * 1441151881L // Based on James Anhalt's algorithm for 9 digits: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
      val y2 = (y1 & 0x1ffffffffffffffL) * 100
      s.append('.').append(((y1 >>> 57).toInt + '0').toChar)
      TimeUtil.append2Digits((y2 >>> 57).toInt, s)
      if ((y2 & 0x1fffff800000000L) != 0) { // check if nano is divisible by 1000000
        val y3 = (y2 & 0x1ffffffffffffffL) * 100
        val y4 = (y3 & 0x1ffffffffffffffL) * 100
        TimeUtil.append2Digits((y3 >>> 57).toInt, s)
        val d = TimeUtil.digits((y4 >>> 57).toInt)
        s.append((d & 0xff).toChar)
        if ((y4 & 0x1ff000000000000L) != 0 || d > 0x3039) { // check if nano is divisible by 1000
          TimeUtil.append2Digits(
            ((y4 & 0x1ffffffffffffffL) * 100 >>> 57).toInt,
            s.append((d >> 8).toChar)
          )
        }
      }
    }

  def to400YearCycle(day: Long): Int =
    (day / 146097).toInt // 146097 == number of days in a 400 year cycle

  def toMarchDayOfYear(marchZeroDay: Long, year: Int): Int = {
    val century =
      (year * 1374389535L >> 37).toInt // divide an int by 100 (the sign correction is not needed)
    (marchZeroDay - year * 365L).toInt - (year >> 2) + century - (century >> 2)
  }

  val digits: Array[Short] = Array(
    0x3030, 0x3130, 0x3230, 0x3330, 0x3430, 0x3530, 0x3630, 0x3730, 0x3830,
    0x3930, 0x3031, 0x3131, 0x3231, 0x3331, 0x3431, 0x3531, 0x3631, 0x3731,
    0x3831, 0x3931, 0x3032, 0x3132, 0x3232, 0x3332, 0x3432, 0x3532, 0x3632,
    0x3732, 0x3832, 0x3932, 0x3033, 0x3133, 0x3233, 0x3333, 0x3433, 0x3533,
    0x3633, 0x3733, 0x3833, 0x3933, 0x3034, 0x3134, 0x3234, 0x3334, 0x3434,
    0x3534, 0x3634, 0x3734, 0x3834, 0x3934, 0x3035, 0x3135, 0x3235, 0x3335,
    0x3435, 0x3535, 0x3635, 0x3735, 0x3835, 0x3935, 0x3036, 0x3136, 0x3236,
    0x3336, 0x3436, 0x3536, 0x3636, 0x3736, 0x3836, 0x3936, 0x3037, 0x3137,
    0x3237, 0x3337, 0x3437, 0x3537, 0x3637, 0x3737, 0x3837, 0x3937, 0x3038,
    0x3138, 0x3238, 0x3338, 0x3438, 0x3538, 0x3638, 0x3738, 0x3838, 0x3938,
    0x3039, 0x3139, 0x3239, 0x3339, 0x3439, 0x3539, 0x3639, 0x3739, 0x3839,
    0x3939
  )

  val daysOfWeek: Array[String] =
    Array("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

  val months: Array[String] =
    Array(
      "Jan",
      "Feb",
      "Mar",
      "Apr",
      "May",
      "Jun",
      "Jul",
      "Aug",
      "Sep",
      "Oct",
      "Nov",
      "Dec"
    )

  def toEpochDay(year: Int, month: Int, day: Int): Long =
    year * 365L + ((year + 3 >> 2) - {
      val cp = year * 1374389535L
      if (year < 0) (cp >> 37) - (cp >> 39) // year / 100 - year / 400
      else
        (cp + 136064563965L >> 37) - (cp + 548381424465L >> 39) // (year + 99) / 100 - (year + 399) / 400
    }.toInt + (month * 1002277 - 988622 >> 15) - // (month * 367 - 362) / 12
      (if (month <= 2) 0
       else if (isLeap(year)) 1
       else 2) + day - 719529) // 719528 == days 0000 to 1970

  def maxDayForYearMonth(year: Int, month: Int): Int =
    if (month != 2) ((month >> 3) ^ (month & 0x1)) + 30
    else if (isLeap(year)) 29
    else 28

  def isLeap(year: Int): Boolean =
    (year & 0x3) == 0 && { // (year % 100 != 0 || year % 400 == 0)
      val cp = year * 1374389535L
      val cc = year >> 31
      ((cp ^ cc) & 0x1fc0000000L) != 0 || (((cp >> 37) - cc) & 0x3) == 0
    }
}
