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

package smithy4s.http

import scala.annotation.tailrec

import PathSegment._
object matchPath extends smithy4s.ScalaCompat {

  def apply(
      path: List[PathSegment],
      received: IndexedSeq[String]
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
        case (ss: StaticSegment) :: lt
            if i < size && compareStrings(ss.value, received(i)) =>
          matchPathAux(lt, i + 1, acc, Nil)
        case (ls: LabelSegment) :: lt if i < size =>
          matchPathAux(lt, i + 1, acc + (ls.value -> received(i)), Nil)
        case (gs: GreedySegment) :: (ss: StaticSegment) :: lt
            if i < size && compareStrings(
              ss.value,
              received(i)
            ) && greedyAcc.nonEmpty =>
          val value = greedyAcc.reverse.mkString("/")
          matchPathAux(lt, i + 1, acc + (gs.value -> value), Nil)
        case p @ ((_: GreedySegment) :: Nil) if i < size =>
          matchPathAux(p, i + 1, acc, received(i) :: greedyAcc)
        case (gs: GreedySegment) :: Nil if greedyAcc.nonEmpty =>
          val value = greedyAcc.reverse.mkString("/")
          Some(acc + (gs.value -> value))
        case _ => None
      }
    matchPathAux(path, 0, Map.empty, List.empty)
  }

  private[http] def make(str: String): IndexedSeq[String] =
    if (str == "" || str == "/")
      IndexedSeq.empty
    else {
      val segments = str.split("/", -1)
      val length = segments.length
      // .head/.last is safe because split always returns non-empty array
      val start = if (segments.head.isEmpty()) 1 else 0
      val end =
        if (length > 1 && segments.last.isEmpty()) length - 1 else length
      val resultArray =
        if (start > 0 || end < length) segments.slice(start, end)
        else segments
      unsafeWrapArray(resultArray)
    }

  private def compareStrings(left: String, right: String): Boolean = {
    (left.hashCode() == right.hashCode()) &&
    left == right
  }

}
