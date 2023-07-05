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
package http

package object internals {

  private[internals] type AwsMergeableHeader[A] = Option[A => String]
  private[internals] type AwsHeaderSplitter[A] = Option[String => Seq[String]]

  private[internals] type HostPrefixEncode[A] =
    smithy4s.codecs.Encoder[List[String], A]
  private[internals] type MaybeHostPrefixEncode[A] = Option[HostPrefixEncode[A]]

  private[http] type HttpCode[A] = A => Option[Int]
  private[http] val httpHints = HintMask(HttpBinding)

  private[http] implicit class vectorOps[A](val vector: Vector[A])
      extends AnyVal {
    def traverse[B](f: A => Option[B]): Option[Vector[B]] =
      vector.foldLeft[Option[Vector[B]]](Some(Vector.empty)) { (result, a) =>
        for {
          acc <- result
          b <- f(a)
        } yield (acc.:+(b))
      }

    def traverse[E, B](f: A => Either[E, B]): Either[E, Vector[B]] =
      vector.foldLeft[Either[E, Vector[B]]](Right(Vector.empty)) {
        (result, a) =>
          for {
            acc <- result
            b <- f(a)
          } yield (acc.:+(b))
      }
  }

  private[internals] implicit class listOps[A](val list: List[A])
      extends AnyVal {
    def traverse[B](f: A => Option[B]): Option[List[B]] =
      list.foldLeft[Option[List[B]]](Some(List.empty)) { (result, a) =>
        for {
          acc <- result
          b <- f(a)
        } yield (acc.:+(b))
      }
  }

  private[http] def pathSegments(
      str: String
  ): Option[Vector[PathSegment]] = {
    str
      .split('?')
      .head
      .split('/')
      .toVector
      .filterNot(_.isEmpty())
      .traverse(fromToString(_))

  }

  private[http] def staticQueryParams(
      uri: String
  ): Map[String, Seq[String]] = {
    uri.split("\\?", 2) match {
      case Array(_) => Map.empty
      case Array(_, query) =>
        query.split("&").toList.foldLeft(Map.empty[String, Seq[String]]) {
          case (acc, param) =>
            val (k, v) = param.split("=", 2) match {
              case Array(key)        => (key, "")
              case Array(key, value) => (key, value)
            }
            acc.updated(k, acc.getOrElse(k, Seq.empty) :+ v)
        }
    }
  }

  private def fromToString(str: String): Option[PathSegment] = {
    if (str == null || str.isEmpty) None
    else if (str.startsWith("{") && str.endsWith("+}"))
      Some(PathSegment.greedy(str.substring(1, str.length() - 2)))
    else if (str.startsWith("{") && str.endsWith("}"))
      Some(PathSegment.label(str.substring(1, str.length() - 1)))
    else Some(PathSegment.static(str))
  }

  private[http] def hostPrefixSegments(
      str: String
  ): Vector[HostPrefixSegment] = {
    // example input: "foo.{bar}--{baz}abcd{test}.com" produces the following
    // output: Vector(static(foo.), label(bar), static(--), label(baz), static(abcd), label(test), static(.com))
    str
      .split('{')
      .toList
      .flatMap(_.split("}", 2).toList match {
        case "" :: Nil     => Nil
        case static :: Nil => HostPrefixSegment.static(static) :: Nil
        case label :: "" :: Nil =>
          HostPrefixSegment.label(label) :: Nil
        case label :: static :: Nil =>
          HostPrefixSegment
            .label(label) :: HostPrefixSegment.static(static) :: Nil
        case _ => Nil
      })
      .toVector
  }

}
