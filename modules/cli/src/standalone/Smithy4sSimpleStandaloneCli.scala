package smithy4s.cli.standalone

import cats.effect.IO
import cats.implicits._
import com.monovore.decline.Opts
import smithy4s.Service
import smithy4s.cli.Entrypoint
import smithy4s.cli.Smithy4sCli
import smithy4s.cli.util.PrinterApi

/** Convenience builder for Smithy4s CLIs that implement their own business logic. Use this if
  * you're the provider of a service and don't want the CLI to be a simple proxy to a remote
  * service, or if you just want to have full control of how you call the upstream.
  *
  * @param impl
  *   Opts providing the implementation of the service. If you have that in a Resource, use use
  *   things like [[smithy4s.cli.util.UnliftResource]] to convert. Only make the Opts require any
  *   parameters if you need program-wide config that doesn't fit into a specific operation's input
  *   (operation inputs are mapped to Opts automatically by all Smithy4sCli implementations).
  */
class Smithy4sSimpleStandaloneCli[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]](
  impl: Opts[smithy4s.Monadic[Alg, IO]]
)(
  implicit service: Service[Alg, Op]
) extends Smithy4sCli[Alg, Op](
    (
      impl,
      PrinterApi.opts.default[IO](),
    ).mapN(Entrypoint.apply[Alg, IO]),
    service,
  )
