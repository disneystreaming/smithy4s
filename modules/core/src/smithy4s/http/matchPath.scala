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

package smithy4s.http

import scala.annotation.tailrec

import PathSegment._

object matchPath {

  def make(str: String): Array[String] =
    if (str == "" || str == "/")
      Array()
    else {
      val segments = str.split("/", -1)
      val length = segments.length
      // .head/.last is safe because split always returns non-empty array
      val start = if (segments.head.isEmpty()) 1 else 0
      val end =
        if (length > 1 && segments.last.isEmpty()) length - 1 else length
      if (start > 0 || end < length) segments.slice(start, end)
      else segments
    }

  private def compareStrings(left: String, right: String): Boolean = {
    (left.hashCode() == right.hashCode()) &&
    left == right
  }

  def apply(
      path: List[PathSegment],
      received: Array[String]
  ): Option[Map[String, String]] = {
    val size = received.length
    @tailrec
    def matchPathAux(
        path: List[PathSegment],
        i: Int,
        acc: Map[String, String],
        greedyAcc: List[String]
    ): Option[Map[String, String]] =
      path match {
        case Nil if i >= size => Some(acc)
        case StaticSegment(value) :: lt if compareStrings(value, received(i)) =>
          matchPathAux(lt, i + 1, acc, Nil)
        case (LabelSegment(name) :: lt) =>
          matchPathAux(lt, i + 1, acc + (name -> received(i)), Nil)
        case (GreedySegment(name) :: StaticSegment(value) :: lt)
            if compareStrings(value, received(i)) && greedyAcc.nonEmpty =>
          val value = greedyAcc.reverse.mkString("/")
          matchPathAux(lt, i + 1, acc + (name -> value), Nil)
        case p @ (GreedySegment(_) :: Nil) if i < size =>
          matchPathAux(p, i + 1, acc, received(i) :: greedyAcc)
        case GreedySegment(name) :: Nil if greedyAcc.nonEmpty =>
          val value = greedyAcc.reverse.mkString("/")
          Some(acc + (name -> value))
        case _ => None
      }
    matchPathAux(path, 0, Map.empty, List.empty)
  }

}
