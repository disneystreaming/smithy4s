/*
 *  Copyright 2021-2023 Disney Streaming
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

package smithy4s.capability
package instances

object option {

  implicit val optionZipper: Zipper[Option] =
    new Zipper[Option] {
      def pure[A](a: A): Option[A] = Some(a)

      override def zipMap[A, B, C](fa: Option[A], fb: Option[B])(
          f: (A, B) => C
      ): Option[C] = (fa, fb) match {
        case (None, _)          => None
        case (_, None)          => None
        case (Some(a), Some(b)) => Some(f(a, b))
      }

      override def zipMapAll[A](
          seq: IndexedSeq[Option[Any]]
      )(f: IndexedSeq[Any] => A): Option[A] = {
        val builder = IndexedSeq.newBuilder[Any]
        var i = 0
        var error: Boolean = false
        while (!error && i < seq.size) {
          seq(i) match {
            case None    => error = true
            case Some(r) => builder += r
          }
          i += 1
        }
        if (error) None
        else Some(f(builder.result()))
      }
    }

}
