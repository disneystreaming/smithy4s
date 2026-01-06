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

package smithy4s.interopcats.instances

import cats.Show
import smithy4s.Blob
import smithy4s.Document
import smithy4s.ShapeId
import smithy4s.kinds.PolyFunction
import smithy4s.schema.Primitive
import smithy4s.time._

import scala.concurrent.duration.Duration

private[interopcats] trait ShowInstances {

  implicit val sId: Show[ShapeId] = Show.fromToString
  implicit val blob: Show[Blob] = (b: Blob) => b.toBase64String
  implicit val document: Show[Document] = Show.fromToString
  implicit val ts: Show[Timestamp] = Show.fromToString
  implicit val localDateShow: Show[LocalDate] = Show.fromToString
  implicit val localTimeShow: Show[LocalTime] = Show.fromToString
  implicit val durationShow: Show[Duration] = Show.fromToString
  implicit val offsetDateTimeShow: Show[OffsetDateTime] = Show.fromToString
  val primShowPf: PolyFunction[Primitive, Show] =
    Primitive.deriving[Show]
}

object ShowInstances extends ShowInstances
