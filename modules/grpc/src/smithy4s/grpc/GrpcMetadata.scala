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

import smithy4s.Blob
import smithy4s.http.CaseInsensitive

final case class GrpcMetadata private (
    values: Map[CaseInsensitive, Vector[GrpcMetadata.Value]]
) {

  def isEmpty: Boolean = values.isEmpty

  def nonEmpty: Boolean = values.nonEmpty

  def keys: Set[CaseInsensitive] = values.keySet

  def get(key: String): Vector[GrpcMetadata.Value] =
    values.getOrElse(CaseInsensitive(key), Vector.empty)

  def getText(key: String): Vector[String] =
    get(key).collect { case GrpcMetadata.Value.Text(value) => value }

  def getBinary(key: String): Vector[Blob] =
    get(key).collect { case GrpcMetadata.Value.Binary(value) => value }

  def add(key: String, value: GrpcMetadata.Value): GrpcMetadata = {
    val k = CaseInsensitive(key)
    val updated = values.getOrElse(k, Vector.empty) :+ value
    GrpcMetadata(values.updated(k, updated))
  }

  def addText(key: String, value: String): GrpcMetadata =
    add(key, GrpcMetadata.Value.Text(value))

  def addBinary(key: String, value: Blob): GrpcMetadata =
    add(key, GrpcMetadata.Value.Binary(value))

  def ++(other: GrpcMetadata): GrpcMetadata =
    other.values.foldLeft(this) { case (acc, (k, vals)) =>
      val updated = acc.values.getOrElse(k, Vector.empty) ++ vals
      GrpcMetadata(acc.values.updated(k, updated))
    }

}

object GrpcMetadata {
  val empty: GrpcMetadata = GrpcMetadata(Map.empty[CaseInsensitive, Vector[GrpcMetadata.Value]])

  def apply(values: (String, Value)*): GrpcMetadata =
    values.foldLeft(empty) { case (acc, (k, v)) => acc.add(k, v) }

  sealed trait Value extends Product with Serializable

  object Value {
    final case class Text(value: String) extends Value
    final case class Binary(value: Blob) extends Value
  }
}
