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

sealed trait PathSegment

object PathSegment {
  def static(value: String): PathSegment = StaticSegment(value)
  def label(value: String): PathSegment = LabelSegment(value)
  def greedy(value: String): PathSegment = GreedySegment(value)

  case class StaticSegment private (value: String) extends PathSegment {
    def withValue(value: String): StaticSegment = {
      copy(value = value)
    }

  }
  object StaticSegment {
    @scala.annotation.nowarn(
      "msg=private method unapply in object StaticSegment is never used"
    )
    private def unapply(c: StaticSegment): Option[StaticSegment] = Some(c)
    def apply(value: String): StaticSegment = {
      new StaticSegment(value)
    }

  }

  case class LabelSegment private (value: String) extends PathSegment {
    def withValue(value: String): LabelSegment = {
      copy(value = value)
    }

  }
  object LabelSegment {
    @scala.annotation.nowarn(
      "msg=private method unapply in object LabelSegment is never used"
    )
    private def unapply(c: LabelSegment): Option[LabelSegment] = Some(c)
    def apply(value: String): LabelSegment = {
      new LabelSegment(value)
    }

  }

  case class GreedySegment private (value: String) extends PathSegment {
    def withValue(value: String): GreedySegment = {
      copy(value = value)
    }

  }
  object GreedySegment {
    @scala.annotation.nowarn(
      "msg=private method unapply in object GreedySegment is never used"
    )
    private def unapply(c: GreedySegment): Option[GreedySegment] = Some(c)
    def apply(value: String): GreedySegment = {
      new GreedySegment(value)
    }

  }

}
