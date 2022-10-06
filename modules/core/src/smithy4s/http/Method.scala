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

package smithy4s.http

sealed trait HttpMethod {
  def showUppercase = this match {
    case HttpMethod.PUT     => "PUT"
    case HttpMethod.POST    => "POST"
    case HttpMethod.DELETE  => "DELETE"
    case HttpMethod.GET     => "GET"
    case HttpMethod.PATCH   => "PATCH"
    case HttpMethod.HEAD    => "HEAD"
    case HttpMethod.OPTIONS => "OPTIONS"
    case HttpMethod.TRACE   => "TRACE"
  }

  def showCapitalised = this match {
    case HttpMethod.PUT     => "Put"
    case HttpMethod.POST    => "Post"
    case HttpMethod.DELETE  => "Delete"
    case HttpMethod.GET     => "Get"
    case HttpMethod.PATCH   => "Patch"
    case HttpMethod.HEAD    => "Head"
    case HttpMethod.OPTIONS => "Options"
    case HttpMethod.TRACE   => "Trace"
  }
}

object HttpMethod {
  case object PUT extends HttpMethod
  case object POST extends HttpMethod
  case object DELETE extends HttpMethod
  case object GET extends HttpMethod
  case object PATCH extends HttpMethod
  case object HEAD extends HttpMethod
  case object OPTIONS extends HttpMethod
  case object TRACE extends HttpMethod

  val values = List(PUT, POST, DELETE, GET, PATCH)

  def fromString(s: String): Option[HttpMethod] = values.find { m =>
    CaseInsensitive(s) == CaseInsensitive(m.showCapitalised)
  }
}
