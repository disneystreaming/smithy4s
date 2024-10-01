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

trait Smithy4sThrowable extends Throwable { self: Product =>

  private def show(message: String): String = {
    val name = getClass().getName()
    if (message == null) {
      val sb = new StringBuilder()
      sb.append(name)
      sb.append("(")
      val iter = this.productIterator
      while (iter.hasNext) {
        sb.append(iter.next())
        if (iter.hasNext) { sb.append(", ") }
      }
      sb.append(")")
      sb.toString()
    } else {
      s"$name: $message"
    }
  }

  override def getMessage(): String = this.show(message = null)

  /**
    * implementing toString, because implementing getMessage
    * lead to `smithy4s.example.ClientError: smithy4s.example.ClientError(400, "oops")`
    * which felt weird
    */
  override def toString(): String = {
    val message = getLocalizedMessage()
    this.show(message = message)
  }
}
