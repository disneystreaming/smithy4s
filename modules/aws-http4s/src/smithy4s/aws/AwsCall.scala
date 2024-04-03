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

package smithy4s.aws

import fs2.Stream
import cats.effect.Resource

// format: off
sealed trait AwsCall[F[_], Input, Err, Output, StreamedInput, StreamedOutput] {

  /**
    * Runs the call and exposes the output in an effect, provided it is proven that the
    * call does not have a streamed component to it.
    */
  def run(implicit ev: AwsOperationKind.Unary[StreamedInput, StreamedOutput]): F[Output]

  /**
    * Uploads a payload and returns an effect, provided it is proven that the call has a
    * streamed input of type Byte, and no streamed output.
    */
  def upload(payload: Stream[F, StreamedInput])(implicit ev: AwsOperationKind.ByteUpload[StreamedInput, StreamedOutput]): F[Output]

  def download(implicit ev: AwsOperationKind.ByteDownload[StreamedInput, StreamedOutput]) : Resource[F, AwsDownloadResult[F, Output, StreamedOutput]]

  def wideUpload[SI]: AwsCall[F, Input, Err, Output, SI, StreamedOutput] = this.asInstanceOf[AwsCall[F, Input, Err, Output, SI, StreamedOutput]]

  def wideDownload[SO]: AwsCall[F, Input, Err, Output, StreamedInput, SO] = this.asInstanceOf[AwsCall[F, Input, Err, Output, StreamedInput, SO]]
}

case class AwsDownloadResult[F[_], O, SO](metadata: O, payload: Stream[F, SO])

object AwsCall {

  private final case class UnaryAwsCall[F[_], Input, Err, Output](run : F[Output]) extends AwsCall[F, Input, Err, Output, Nothing, Nothing]{
    def run(implicit ev: AwsOperationKind.Unary[Nothing,Nothing]): F[Output] = run
    def upload(payload: Stream[F, Nothing])(implicit ev: AwsOperationKind.ByteUpload[Nothing,Nothing]): F[Output] = sys.error("Impossible call")
    def download(implicit ev: AwsOperationKind.ByteDownload[Nothing,Nothing]): Resource[F,AwsDownloadResult[F,Output,Nothing]] = sys.error("Impossible calls")
  }

  private final case class BlobUploadAwsCall[F[_], Input, Err, Output, StreamedInput](uploadFunction: Stream[F, StreamedInput] => F[Output]) extends AwsCall[F, Input, Err, Output, StreamedInput, Nothing]{
    def run(implicit ev: AwsOperationKind.Unary[StreamedInput,Nothing]): F[Output] = sys.error("Impossible call")
    def upload(payload: Stream[F, StreamedInput])(implicit ev: AwsOperationKind.ByteUpload[StreamedInput,Nothing]): F[Output] = uploadFunction(payload)
    def download(implicit ev: AwsOperationKind.ByteDownload[StreamedInput,Nothing]): Resource[F,AwsDownloadResult[F,Output,Nothing]] = sys.error("Impossible call")
  }

  def download[F[_], Input, Err, Output, StreamedOutput](res: (Byte => StreamedOutput) => Resource[F, AwsDownloadResult[F, Output, StreamedOutput]]): AwsCall[F, Input, Err, Output, Nothing, StreamedOutput] = new BlobDownloadAwsCall(res)

  private final case class BlobDownloadAwsCall[F[_], Input, Err, Output, StreamedOutput](downloadResult: (Byte => StreamedOutput) => Resource[F, AwsDownloadResult[F, Output, StreamedOutput]]) extends AwsCall[F, Input, Err, Output, Nothing, StreamedOutput]{
    def run(implicit ev: AwsOperationKind.Unary[Nothing,StreamedOutput]): F[Output] = sys.error("Impossible call")
    def upload(payload: Stream[F, Nothing])(implicit ev: AwsOperationKind.ByteUpload[Nothing,StreamedOutput]): F[Output] = sys.error("Impossible call")
    def download(implicit ev: AwsOperationKind.ByteDownload[Nothing,StreamedOutput]): Resource[F,AwsDownloadResult[F,Output,StreamedOutput]] = downloadResult(ev.apply)
  }





}
