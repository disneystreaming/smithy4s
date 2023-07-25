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
package schema

/**
  * Represents a member of product type (case class)
  */
sealed abstract class Field[S, A] {

  val label: String
  val schema: Schema[A]
  val get: S => A

  /**
    * Returns the hints that are only relative to the field
    * (typically derived from member-level traits)
    */
  final def memberHints: Hints = schema.hints.memberHints

  /**
    * Returns all hints : the ones defined directly on the field, and the ones
    * defined on the target of the field.
    */
  final def hints: Hints = schema.hints

  @deprecated("use .schema instead", since = "0.18.0")
  final def instance: Schema[A] = schema

  lazy val getDefaultValue: Option[A] =
    schema.getDefaultValue

  def isDefaultValue(a: A): Boolean = getDefaultValue.contains(a)

  def getUnlessDefault(s: S): Option[A] = {
    val a = get(s)
    getDefaultValue match {
      case Some(`a`) => None
      case _         => Some(a)
    }
  }

  def hasDefaultValue: Boolean = getDefaultValue.isDefined
  def isStrictlyRequired: Boolean = !hasDefaultValue

  def transformHintsLocally(f: Hints => Hints): Field[S, A] =
    Field.GetterField(label, schema.transformHintsLocally(f), get)

  def transformHintsTransitively(f: Hints => Hints) =
    Field.GetterField(label, schema.transformHintsTransitively(f), get)

  def contramap[S0](f: S0 => S): Field[S0, A] =
    Field.GetterField(label, schema, get.compose(f))

  def addHints(newHints: Hint*): Field[S, A] =
    Field.GetterField(label, schema.addMemberHints(newHints: _*), get)
}

object Field {

  def apply[S, A](
      label: String,
      schema: Schema[A],
      get: S => A
  ): Field[S, A] =
    GetterField(label, schema, get)

  def optional[S, A](
      label: String,
      schema: Schema[A],
      get: S => Option[A]
  ): Field[S, Option[A]] =
    GetterField(label, schema.option, get)

  private[schema] case class GetterField[S, A](
      label: String,
      schema: Schema[A],
      get: S => A
  ) extends Field[S, A]

  private case class LensField[S, A](
      label: String,
      schema: Schema[A],
      get: S => A,
      replace: A => (S => S)
  ) extends Field[S, A]
      with smithy4s.optics.Lens[S, A] {
    def get(s: S): A = get.apply(s)
    def replace(a: A): S => S = replace.apply(a)
  }

}
