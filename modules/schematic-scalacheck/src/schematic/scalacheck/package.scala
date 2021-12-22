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

package object scalacheck {

  type DynData = Any
  type DynStruct = Map[String, DynData]
  type DynAlt = (String, DynData)

  object gen extends SchematicGen

  private[scalacheck] def distinctBy[A, B](
      list: Vector[A]
  )(f: A => B): Vector[A] = {
    val start = (Vector.empty[A], Set.empty[B])
    val (res, _) = list.foldLeft(start) {
      case ((res, track), a) if track(f(a)) => (res, track)
      case ((res, track), a)                => (res :+ a, track + f(a))
    }
    res
  }

}
