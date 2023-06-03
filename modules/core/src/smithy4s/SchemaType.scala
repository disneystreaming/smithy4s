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

sealed trait SchemaType

/**
  * Tag for types used in Smithy4s schemas.
  * Used to allow default schema visitor implementations to know what type of
  * schema they are visiting.
  */
object SchemaType {
  case object Primitive extends SchemaType
  case object Collection extends SchemaType
  case object Map extends SchemaType
  case object Enumeration extends SchemaType
  case object Struct extends SchemaType
  case object Union extends SchemaType
  case object Bijection extends SchemaType
  case object Refinement extends SchemaType
  case object Lazily extends SchemaType
  case object Nullable extends SchemaType
}
