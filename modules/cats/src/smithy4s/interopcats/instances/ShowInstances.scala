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

package smithy4s.interopcats.instances

import cats.Show
import smithy4s.schema.Primitive
import smithy4s.{Blob, Document, ShapeId, Timestamp}
import smithy4s.kinds.PolyFunction

private[interopcats] trait ShowInstances {

  implicit val sId: Show[ShapeId] = Show.fromToString
  implicit val blob: Show[Blob] = (b: Blob) => b.toBase64String
  implicit val document: Show[Document] = Show.fromToString
  implicit val ts: Show[Timestamp] = Show.fromToString
  val primShowPf: PolyFunction[Primitive, Show] =
    Primitive.deriving[Show]
}

object ShowInstances extends ShowInstances
