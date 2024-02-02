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

final case class HttpUri(
    scheme: HttpUriScheme,
    host: Option[String],
    port: Option[Int],
    /**
      * A sequence of URL-decoded URI segment.
      */
    path: IndexedSeq[String],
    queryParams: Map[String, Seq[String]],
    /**
      * Field allowing to store decoded path parameters alongside an http request,
      * once the routing logic has come in effect.
      */
    pathParams: Option[Map[String, String]]
)

object HttpUri {}
