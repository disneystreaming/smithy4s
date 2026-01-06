/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.http.internals

import smithy4s.capability.Contravariant

trait PathEncode[A] { self =>
  def encode(a: A): List[String]
  def encodeGreedy(a: A): List[String]

  def contramap[B](from: B => A): PathEncode[B] = new PathEncode[B] {
    override def encode(b: B): List[String] =
      self.encode(from(b))
    override def encodeGreedy(b: B): List[String] =
      self.encodeGreedy(from(b))
  }
}

object PathEncode {

  type MaybePathEncode[A] = Option[PathEncode[A]]

  implicit val contravariantInstance: Contravariant[PathEncode] =
    new Contravariant[PathEncode] {
      def contramap[A, B](fa: PathEncode[A])(f: B => A): PathEncode[B] =
        fa.contramap(f)
    }
  def raw[A](f: A => String, urlEncode: Boolean): PathEncode[A] = {
    new PathEncode[A] {
      override def encode(a: A): List[String] = {
        val initial = f(a)
        List {
          if (urlEncode) encodeUnreserved(initial, false)
          else initial
        }
      }

      override def encodeGreedy(a: A): List[String] = {
        val initial = f(a)
        (if (urlEncode) encodeUnreserved(initial, true)
         else initial)
          .split('/')
          .toList
      }
    }
  }

  def from[A](f: A => String, urlEncode: Boolean): MaybePathEncode[A] = {
    Some {
      raw(f, urlEncode)
    }
  }
  def fromToString[A](urlEncode: Boolean): MaybePathEncode[A] =
    from(_.toString, urlEncode)

  /**
   * Encodes characters that are not unreserved into a string builder.
   * [[https://github.com/smithy-lang/smithy-java/blob/7cf74ae295480454d00e905053a212a77cbde34b/io/src/main/java/software/amazon/smithy/java/io/uri/URLEncoding.java#L14 Ported from smithy's `software.amazon.smithy.java.io.uri.URLEncoding`]].
   * <p>
   * <code>
   * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
   * </code>
   * Can optionally handle strings which are meant to encode a path (ie include '/' which should NOT be escaped for paths).
   *
   * @param source        The raw string to encode. Note that any existing percent-encoding will be encoded again.
   * @param ignoreSlashes true if the value is intended to represent a path.
   */
  private def encodeUnreserved(
      source: String,
      ignoreSlashes: Boolean
  ): String = {
    // Encode the path segment and undo some of the assumption of URLEncoder to make it with unreserved.
    val encoded = java.net.URLEncoder.encode(source, "UTF-8")
    val sink = new StringBuilder(encoded.length)
    var i = 0
    while (i < encoded.length) {
      val c = encoded.charAt(i)
      c match {
        case '+' => sink.append("%20")
        case '*' => sink.append("%2A")
        case '%' =>
          encoded.charAt(i + 1) match {
            case '7'
                if (i < encoded.length - 1 && encoded.charAt(i + 2) == 'E') =>
              sink.append('~')
              i += 2

            case '2'
                if (i < encoded.length - 1 && encoded.charAt(
                  i + 2
                ) == 'F' && ignoreSlashes) =>
              sink.append('/')
              i += 2

            case _ => sink.append(c)
          }

        case _ => sink.append(c)
      }

      i += 1
    }

    sink.toString
  }
}
