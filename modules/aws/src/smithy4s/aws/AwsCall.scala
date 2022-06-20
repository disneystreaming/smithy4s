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
    * call does not have a streaming component to it.
    */
  def run(implicit ev: AwsOperationKind.Unary[StreamedInput, StreamedOutput]): F[Output]

  // def upload(input: Stream[F, StreamedInput], size: Option[Long] = None)(implicit ev2: StreamedOutput =:= Nothing): F[Result]
  // def download(implicit ev2: StreamedInput =:= Nothing): Resource[F, Output[F, Result, StreamedOutput]]
  // def pipe(input: Stream[F, StreamedInput], size: Option[Long] = None): Resource[F, Output[F, Result, StreamedOutput]]
}

