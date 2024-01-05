/*
 *  Copyright 2021-2024 Disney Streaming
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

import scala.util.control.NoStackTrace

package object internals {
  type SchemaDescription[A] = String
  val SchemaDescriptionDetailed: Schema ~> SchemaDescription =
    SchemaDescriptionDetailedImpl.andThen(
      SchemaDescriptionDetailedImpl.conversion
    )

  private object DoneEarly extends Exception with NoStackTrace

  private[internals] implicit class vectorOps[A](val vector: Vector[A])
      extends AnyVal {
    def traverse[B](f: A => Option[B]): Option[Vector[B]] = {
      val vec = Vector.newBuilder[B]
      var doneEarly = false
      vector.foreach { a =>
        f(a) match {
          case Some(b) => vec += b
          case None =>
            doneEarly = true
            scala.util.control.Breaks.break()
        }
      }

      if (doneEarly) None else Some(vec.result())
    }
  }
}
