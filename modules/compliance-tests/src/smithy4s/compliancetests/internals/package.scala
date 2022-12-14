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

package smithy4s.compliancetests

import smithy4s.Hints
import smithy4s.schema.Schema

package object internals {

  // Due to AWS's usage of integer as the canonical representation of a Timestamp in smithy , we need to provide the decoder with instructions to use a Long instead.
  // therefore the timestamp type is switched to type epochSeconds: Long
  // This is just a workaround thats limited to testing scenarios
  def mapAllTimestampsToEpoch[A](schema: Schema[A]): Schema[A] = {
    schema.transformHintsTransitively(h =>
      h.++(Hints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen))
    )
  }
}
