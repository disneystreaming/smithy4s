/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.grpc

import scala.concurrent.duration._

final case class GrpcTimeout(amount: Long, unit: GrpcTimeout.Unit) {
  def toHeader: String = s"$amount${unit.suffix}"
  def toFiniteDuration: FiniteDuration = unit.toFiniteDuration(amount)
}

object GrpcTimeout {

  sealed trait Unit {
    def suffix: Char
    def toFiniteDuration(amount: Long): FiniteDuration
  }

  case object Nanoseconds extends Unit {
    val suffix: Char = 'n'
    def toFiniteDuration(amount: Long): FiniteDuration = amount.nanos
  }

  case object Microseconds extends Unit {
    val suffix: Char = 'u'
    def toFiniteDuration(amount: Long): FiniteDuration = amount.micros
  }

  case object Milliseconds extends Unit {
    val suffix: Char = 'm'
    def toFiniteDuration(amount: Long): FiniteDuration = amount.millis
  }

  case object Seconds extends Unit {
    val suffix: Char = 'S'
    def toFiniteDuration(amount: Long): FiniteDuration = amount.seconds
  }

  case object Minutes extends Unit {
    val suffix: Char = 'M'
    def toFiniteDuration(amount: Long): FiniteDuration = amount.minutes
  }

  case object Hours extends Unit {
    val suffix: Char = 'H'
    def toFiniteDuration(amount: Long): FiniteDuration = amount.hours
  }

  sealed trait ParseError extends Product with Serializable {
    def message: String
  }

  object ParseError {
    final case class InvalidFormat(value: String) extends ParseError {
      val message: String = s"Invalid grpc-timeout: $value"
    }

    final case class InvalidUnit(value: String) extends ParseError {
      val message: String = s"Invalid grpc-timeout unit: $value"
    }
  }

  def parse(value: String): Either[ParseError, GrpcTimeout] = {
    if (value.length < 2) Left(ParseError.InvalidFormat(value))
    else {
      val (digits, suffixStr) = value.splitAt(value.length - 1)
      if (digits.isEmpty || !digits.forall(_.isDigit) || digits.length > 8)
        Left(ParseError.InvalidFormat(value))
      else {
        val unit = suffixStr.charAt(0) match {
          case 'n' => Some(Nanoseconds)
          case 'u' => Some(Microseconds)
          case 'm' => Some(Milliseconds)
          case 'S' => Some(Seconds)
          case 'M' => Some(Minutes)
          case 'H' => Some(Hours)
          case _   => None
        }
        unit.toRight(ParseError.InvalidUnit(value)).flatMap { u =>
          scala.util.Try(digits.toLong).toEither
            .left.map(_ => ParseError.InvalidFormat(value))
            .flatMap { amount =>
              if (amount <= 0) Left(ParseError.InvalidFormat(value))
              else Right(GrpcTimeout(amount, u))
            }
        }
      }
    }
  }
}
