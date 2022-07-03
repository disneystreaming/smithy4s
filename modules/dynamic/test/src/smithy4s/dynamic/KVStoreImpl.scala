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
package dynamic

import smithy4s.example._
import DummyIO._

class KVStoreImpl(map: scala.collection.mutable.Map[String, String])
    extends KVStore[IO] {
  def delete(key: String): IO[Unit] =
    map.remove(key) match {
      case None    => Left(KeyNotFoundError(key))
      case Some(_) => Right(())
    }
  def get(key: String): IO[Value] =
    if (key.contains("authorized-only"))
      // This will be redacted by the redacting proxy
      IO.raiseError(UnauthorizedError("sensitive"))
    else
      map.get(key) match {
        case None        => Left(KeyNotFoundError(key))
        case Some(value) => Right(smithy4s.example.Value(value))
      }
  def put(key: String, value: String): IO[Unit] = IO {
    val _ = map.put(key, value)
  }
}
