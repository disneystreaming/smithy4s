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

import munit._
import smithy4s.example.ObjectCollision

class ObjectCollisionSmokeSpec() extends FunSuite {

  test(
    "Operations colliding with java.lang.Object methods get rendered with a leading underscore"
  ) {
    // Here we're just testing that the operations gets rendered as expected by leveraging the compiler.
    object Test extends ObjectCollision[cats.Id] {
      override def _equals(): Unit = ()
      override def _clone(): Unit = ()
      override def _finalize(): Unit = ()
      override def _getClass(): Unit = ()
      override def _hashCode(): Unit = ()
      override def _notifyAll(): Unit = ()
      override def _notify(): Unit = ()
      override def _toString(): Unit = ()
      override def _wait(): Unit = ()

    }
    val _ = Test
  }

}
