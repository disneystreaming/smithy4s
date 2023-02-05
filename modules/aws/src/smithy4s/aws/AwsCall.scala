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

// format: off
trait AwsCall[F[_], Input, Err, Output, StreamedInput, StreamedOutput] {

  /**
    * Runs the call and exposes the output in an effect, provided it is proven that the
    * call does not have a streamed component to it.
    */
  def run(implicit ev: AwsOperationKind.Unary[StreamedInput, StreamedOutput]): F[Output]

  // /**
  //   * Uploads a payload and returns an effect, provided it is proven that the call has a
  //   * streamed input of type Byte, and no streamed output.
  //   */
  // def upload[P](payload: P)(implicit uploadable: AwsUploadable[F, P], ev: AwsOperationKind.ByteUpload[StreamedInput, StreamedOutput]): F[Output]
}
