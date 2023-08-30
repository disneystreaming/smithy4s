package smithy4s.client

// scalafmt: { maxColumn = 120}
trait UnaryLowLevelClient[F[_], Request, Response] { self =>
  def run[Output](request: Request)(responseCB: Response => F[Output]): F[Output]
}
