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
import smithy4s.Nullable._

/**
 * ADT isomorphic to `Option`, but representing types that were passed explicitly as
 * null rather than ones that were absent.
 *
 * The goal of this datatype is to offer the ability to distinguish, during
 * serialisation, between the absence of a field and the nullity of a field.
 */
sealed trait Nullable[+A] {
  def map[B](f: A => B): Nullable[B] = this match {
    case Value(a) => Value(f(a))
    case Null     => Null
  }

  def fold[B](whenNull: => B)(whenValue: A => B): B =
    this match {
      case Value(a) => whenValue(a)
      case Null     => whenNull
    }

  def toOption: Option[A] = this match {
    case Value(a) => Some(a)
    case Null     => None
  }
}

object Nullable {

  def value[A](a: A): Nullable[A] = Value(a)

  def fromOption[A](option: Option[A]) = option match {
    case Some(a) => Value(a)
    case None    => Null
  }

  final case class Value[A](a: A) extends Nullable[A]
  case object Null extends Nullable[Nothing]

  private[smithy4s] def schema[A](schemaA: Schema[A]): Schema[Nullable[A]] = {
    schemaA.addMemberHints(alloy.Nullable()).option.biject {
      Bijection[Option[A], Nullable[A]](fromOption, _.toOption)
    }
  }

  private[smithy4s] object Schema {
    import smithy4s.Schema.{BijectionSchema, OptionSchema}

    def unapply[A](schema: Schema[A]): Option[Schema[_]] = {
      schema match {
        case bs: BijectionSchema[_, _] if bs.hints.has(alloy.Nullable) =>
          bs.underlying match {
            case os: OptionSchema[_] => Some(os.underlying)
            case _                        => None
          }
        case _ => None
      }
    }
  }
}
