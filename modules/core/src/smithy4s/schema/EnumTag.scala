/*
 *  Copyright 2021-2024 Disney Streaming
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

  case class OpenStringEnum[E] private (unknown: String => E)
      extends EnumTag[E] {
    def withUnknown(value: String => E): OpenStringEnum[E] = {
      copy(unknown = value)
    }

  }
  object OpenStringEnum {
    @scala.annotation.nowarn(
      "msg=private method unapply in object OpenStringEnum is never used"
    )
    private def unapply[E](c: OpenStringEnum[E]): Option[OpenStringEnum[E]] =
      Some(
        c
      )
    def apply[E](unknown: String => E): OpenStringEnum[E] = {
      new OpenStringEnum(unknown)
    }
  }

  case class OpenIntEnum[E] private (unknown: Int => E) extends EnumTag[E] {
    def withUnknown(value: Int => E): OpenIntEnum[E] = {
      copy(unknown = value)
    }

  }
  object OpenIntEnum {
    @scala.annotation.nowarn(
      "msg=private method unapply in object OpenIntEnum is never used"
    )
    private def unapply[E](c: OpenIntEnum[E]): Option[OpenIntEnum[E]] = Some(c)
    def apply[E](unknown: Int => E): OpenIntEnum[E] = {
      new OpenIntEnum(unknown)
    }
  }

  object StringEnum {
    def unapply[E](enumTag: EnumTag[E]): Boolean = enumTag match {
      case ClosedStringEnum     => true
      case _: OpenStringEnum[_] => true
      case _                    => false
    }
  }

  object IntEnum {
    def unapply[E](enumTag: EnumTag[E]): Boolean = enumTag match {
      case ClosedIntEnum     => true
      case _: OpenIntEnum[_] => true
      case _                 => false
    }
  }
}
