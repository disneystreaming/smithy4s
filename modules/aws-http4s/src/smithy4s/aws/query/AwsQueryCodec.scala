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

package smithy4s.aws.query

private[aws] trait AwsQueryCodec[-A] extends (A => FormData) { self =>
  def apply(a: A): FormData
  def contramap[B](f: B => A): AwsQueryCodec[B] = new AwsQueryCodec[B] {
    def apply(b: B): FormData = self(f(b))
  }

  def prepend(key: String): AwsQueryCodec[A] = new AwsQueryCodec[A] {
    def apply(a: A): FormData = self(a).prepend(key)
  }
}
