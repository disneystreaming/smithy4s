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
final case class Field[S, A](
    label: String,
    targetSchema: Schema[A],
    get: S => A,
    localHints: Hints
) {

  /**
    * Returns the target schema, amended to carry the local hints as if they
    * had been defined directly on the target shape. Most of the time, this is
    * the desired behaviour when writing SchemaVisitors.
    */
  def schema: Schema[A] = targetSchema.addHints(localHints)

  /**
    * Returns all hints : the ones defined directly on the field, and the ones
    * defined on the target of the field.
    */
  final def hints = targetSchema.hints ++ localHints

  // TODO : rename
  @deprecated("use .schema instead", since = "0.18.0")
  final def instance: Schema[A] = schema.addHints(localHints)
  def isRequired: Boolean = hints.has(smithy.api.Required)
  def isOptional: Boolean = !isRequired
  lazy val getDefaultValue: Option[A] =
    schema.getDefaultValue

  def isDefaultValue(a: A): Boolean = getDefaultValue.contains(a)

  def getIfNonDefault(s: S): Option[A] = {
    val a = get(s)
    getDefaultValue match {
      case Some(`a`) => None
      case _         => Some(a)
    }
  }

  def hasDefaultValue(s: S): Boolean = isDefaultValue(get(s))

  def transformHintsLocally(f: Hints => Hints): Field[S, A] =
    copy(localHints = f(localHints))

  def transformHintsTransitively(f: Hints => Hints) =
    copy(
      localHints = f(localHints),
      targetSchema = targetSchema.transformHintsTransitively(f)
    )

  def contramap[S0](f: S0 => S): Field[S0, A] =
    Field(label, targetSchema, get.compose(f), localHints)

  def addHints(newHints: Hint*): Field[S, A] =
    copy(localHints = this.localHints ++ Hints(newHints: _*))
}

object Field {

  def required[S, A](
      label: String,
      schema: Schema[A],
      get: S => A
  ): Field[S, A] =
    Field(label, schema, get, Hints.empty)

  def optional[S, A](
      label: String,
      schema: Schema[A],
      get: S => Option[A]
  ): Field[S, Option[A]] =
    Field(label, schema.nullable, get, Hints.empty)

}
