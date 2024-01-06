package object weather {
  type WeatherService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[WeatherServiceGen, F]
  val WeatherService = WeatherServiceGen


}