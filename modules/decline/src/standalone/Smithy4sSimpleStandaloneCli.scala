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

package smithy4s.decline.standalone

import cats.implicits._
import cats.MonadThrow
import cats.effect.std.Console
import com.monovore.decline.Opts
import smithy4s.Service
import smithy4s.decline.Entrypoint
import smithy4s.decline.Smithy4sCli
import smithy4s.decline.util.PrinterApi

/** Convenience builder for Smithy4s CLIs that implement their own business logic. Use this if
  * you're the provider of a service and don't want the CLI to be a simple proxy to a remote
  * service, or if you just want to have full control of how you call the upstream.
  *
  * @param impl
  *   Opts providing the implementation of the service. If you have that in a Resource, use use
  *   things like [[smithy4s.decline.util.UnliftResource]] to convert. Only make the Opts require any
  *   parameters if you need program-wide config that doesn't fit into a specific operation's input
  *   (operation inputs are mapped to Opts automatically by all Smithy4sCli implementations).
  */
class Smithy4sSimpleStandaloneCli[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _], F[
    _
]: Console: MonadThrow](
    impl: Opts[smithy4s.Monadic[Alg, F]]
)(implicit
    service: Service[Alg, Op]
) extends Smithy4sCli[Alg, Op, F](
      (
        impl,
        PrinterApi.opts.default[F]()
      ).mapN(Entrypoint.apply[Alg, F]),
      service
    )
