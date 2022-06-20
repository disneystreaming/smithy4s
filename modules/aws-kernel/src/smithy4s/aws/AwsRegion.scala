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

package smithy4s.aws.kernel

import smithy4s.Newtype

object AwsRegion extends Newtype[String] {

  val id = smithy4s.ShapeId("smithy4s.aws", "AwsRegion")

  val AF_SOUTH_1: AwsRegion = apply("af-south-1")
  val AP_EAST_1: AwsRegion = apply("ap-east-1")
  val AP_NORTHEAST_1: AwsRegion = apply("ap-northeast-1")
  val AP_NORTHEAST_2: AwsRegion = apply("ap-northeast-1")
  val AP_NORTHEAST_3: AwsRegion = apply("ap-northeast-2")
  val AP_SOUTH_1: AwsRegion = apply("ap-south-1")
  val AP_SOUTHEAST_1: AwsRegion = apply("ap-southeast-1")
  val AP_SOUTHEAST_2: AwsRegion = apply("ap-southeast-2")
  val CA_CENTRAL_1: AwsRegion = apply("ca-central-1")
  val CN_NORTH_1: AwsRegion = apply("cn-north-1")
  val CN_NORTHWEST_1: AwsRegion = apply("cn-northwest-1")
  val EU_CENTRAL_1: AwsRegion = apply("eu-central-1")
  val EU_NORTH_1: AwsRegion = apply("eu-north-1")
  val EU_SOUTH_1: AwsRegion = apply("eu-south-1")
  val EU_WEST_1: AwsRegion = apply("eu-west-1")
  val EU_WEST_2: AwsRegion = apply("eu-west-2")
  val EU_WEST_3: AwsRegion = apply("eu-west-3")
  val GovCloud: AwsRegion = apply("govcloud")
  val ME_SOUTH_1: AwsRegion = apply("me-south-1")
  val SA_EAST_1: AwsRegion = apply("sa-east-1")
  val US_EAST_1: AwsRegion = apply("us-east-1")
  val US_EAST_2: AwsRegion = apply("us-east-2")
  val US_GOV_EAST_1: AwsRegion = apply("us-gov-east-1")
  val US_ISO_EAST_1: AwsRegion = apply("us-iso-east-1")
  val US_ISO_WEST_1: AwsRegion = apply("us-iso-west-1")
  val US_ISOB_EAST_1: AwsRegion = apply("us-isob-east-1")
  val US_WEST_1: AwsRegion = apply("us-west-1")
  val US_WEST_2: AwsRegion = apply("us-west-2")

}
