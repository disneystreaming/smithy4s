package object weather {
  type WeatherService[F[_]] = smithy4s.kinds.FunctorAlgebra[WeatherServiceGen, F]
  val WeatherService = WeatherServiceGen


}