/*
 *  Copyright 2021 Disney Streaming
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
package dynamic

import smithy4s.example._
import cats.effect.IO
import cats.effect.Ref
import cats.syntax.all._

class KVStoreImpl(ref: Ref[IO, Map[String, String]]) extends KVStore[IO] {
  def delete(key: String): IO[Unit] = ref
    .modify[Either[Throwable, Unit]] { map =>
      map.get(key) match {
        case None        => (map, Left(KeyNotFoundError(key)))
        case Some(value) => (map - value, Right(()))
      }
    }
    .rethrow
  def get(key: String): IO[Value] =
    ref.get
      .map[Either[Throwable, Value]](_.get(key) match {
        case None        => Left(KeyNotFoundError(key))
        case Some(value) => Right(smithy4s.example.Value(value))
      })
      .rethrow
  def put(key: String, value: String): IO[Unit] = ref.update(_ + (key -> value))
}
