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

package smithy4s.http

import scala.collection.{Map => MMap}
import scala.annotation.nowarn

final class MetadataPartial[A] private[smithy4s] (
    private[smithy4s] val decoded: MMap[String, Any]
) {
  @deprecated(
    "0.16.8",
    "This may throw a PayloadError. Use combineCatch instead."
  )
  final def combine(body: BodyPartial[A]): A = body.complete(decoded)

  final def combineCatch(body: BodyPartial[A]): Either[PayloadError, A] =
    body.completeCatch(decoded)
}
object MetadataPartial {
  private[smithy4s] def apply[A](decoded: MMap[String, Any]) =
    new MetadataPartial[A](decoded)
}

final class BodyPartial[A] private[smithy4s] (
    private[smithy4s] val complete: MMap[String, Any] => A
) {
  @deprecated(
    "0.16.8",
    "This may throw a PayloadError. Use combineCatch instead."
  )
  @nowarn("msg=method combine in class MetadataPartial is deprecated")
  final def combine(metadata: MetadataPartial[A]): A =
    metadata.combine(this)

  final def combineCatch(
      metadata: MetadataPartial[A]
  ): Either[PayloadError, A] =
    metadata.combineCatch(this)

  final def map[B](f: A => B): BodyPartial[B] = new BodyPartial(
    complete andThen f
  )

  @nowarn("msg=method combine in class BodyPartial is deprecated")
  private[smithy4s] def completeCatch(
      m: MMap[String, Any]
  ): Either[PayloadError, A] = try Right(complete(m))
  catch {
    case p: PayloadError => Left(p)
  }
}
object BodyPartial {
  def apply[A](complete: MMap[String, Any] => A): BodyPartial[A] =
    new BodyPartial[A](complete)
}
