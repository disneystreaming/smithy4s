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

package smithy4s.schema

sealed trait EnumTag[+E]

object EnumTag {
  case object ClosedStringEnum extends EnumTag[Nothing]
  case object ClosedIntEnum extends EnumTag[Nothing]
  case class OpenStringEnum[E](unknown: String => E) extends EnumTag[E]
  case class OpenIntEnum[E](unknown: Int => E) extends EnumTag[E]

  object StringEnum {
    def unapply[E](enumTag: EnumTag[E]): Boolean = enumTag match {
      case ClosedStringEnum  => true
      case OpenStringEnum(_) => true
      case _                 => false
    }

  }

  object IntEnum {
    def unapply[E](enumTag: EnumTag[E]): Boolean = enumTag match {
      case ClosedIntEnum  => true
      case OpenIntEnum(_) => true
      case _              => false
    }
  }
}
