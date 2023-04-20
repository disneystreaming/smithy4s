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

import smithy4s.errors.*

object Main extends App {
  val serviceOps = ErrorServiceOperation
  val knownError1 = BadRequest("foo")
  val knownError2 = InternalServerError("bar")
  val unknownError = new RuntimeException("baz")
  assert(serviceOps.ErrorOp.liftError(unknownError) == None)
  assert(serviceOps.ErrorOp.liftError(knownError1) == Some(knownError1))
  assert(serviceOps.ErrorOp.liftError(knownError2) == Some(knownError2))
  assert(serviceOps.ErrorOp.unliftError(knownError1) == knownError1)
}
