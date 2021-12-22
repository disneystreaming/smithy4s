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

package schematic


import java.util.UUID

object uuid {

  object Schema extends schematic.Schema[Schematic, UUID] {
    def compile[F[_]](s: Schematic[F]): F[UUID] = s.uuid
  }

  trait Schematic[F[_]] {
    def uuid: F[UUID]
  }

  trait Syntax {
    val uuid : schematic.Schema[Schematic, UUID] = Schema
  }

}
