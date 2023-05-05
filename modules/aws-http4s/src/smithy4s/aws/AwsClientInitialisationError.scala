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

package smithy4s.aws

import smithy4s.ShapeId
import smithy4s.ShapeTag

sealed trait AwsClientInitialisationError extends Exception
object AwsClientInitialisationError {
  case class NotAws(serviceId: ShapeId)
      extends Exception(s"${serviceId.show} is not an AWS service")
      with AwsClientInitialisationError

  case class UnsupportedProtocol(
      serviceId: ShapeId,
      knownProtocols: List[ShapeTag[_]]
  ) extends Exception(
        s"AWS protocol used by ${serviceId.show} is not yet supported"
      )
      with AwsClientInitialisationError

}
