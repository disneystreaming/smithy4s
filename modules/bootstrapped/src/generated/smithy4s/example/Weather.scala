package smithy4s.example

import smithy4s.Endpoint
import smithy4s.Errorable
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.StreamingSchema
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.Schema.UnionSchema
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union
import smithy4s.schema.Schema.unit

/** Provides weather forecasts. */
trait WeatherGen[F[_, _, _, _, _]] {
  self =>

  def getCurrentTime(): F[Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing]
  def getCity(cityId: CityId): F[GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing]
  def getForecast(cityId: CityId): F[GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing]
  def listCities(nextToken: Option[String] = None, pageSize: Option[Int] = None): F[ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[WeatherGen[F]] = Transformation.of[WeatherGen[F]](this)
}

object WeatherGen extends Service.Mixin[WeatherGen, WeatherOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "Weather")
  val version: String = "2006-03-01"

  val hints: Hints = Hints(
    smithy.api.Documentation("Provides weather forecasts."),
    smithy.api.Paginated(inputToken = Some(smithy.api.NonEmptyString("nextToken")), outputToken = Some(smithy.api.NonEmptyString("nextToken")), items = None, pageSize = Some(smithy.api.NonEmptyString("pageSize"))),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: List[smithy4s.Endpoint[WeatherOperation, _, _, _, _, _]] = List(
    WeatherOperation.GetCurrentTime,
    WeatherOperation.GetCity,
    WeatherOperation.GetForecast,
    WeatherOperation.ListCities,
  )

  def endpoint[I, E, O, SI, SO](op: WeatherOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends WeatherOperation.Transformed[WeatherOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: WeatherGen[WeatherOperation] = WeatherOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: WeatherGen[P], f: PolyFunction5[P, P1]): WeatherGen[P1] = new WeatherOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[WeatherOperation, P]): WeatherGen[P] = new WeatherOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: WeatherGen[P]): PolyFunction5[WeatherOperation, P] = WeatherOperation.toPolyFunction(impl)

  type GetCityError = WeatherOperation.GetCityError
  val GetCityError = WeatherOperation.GetCityError
}

sealed trait WeatherOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: WeatherGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def endpoint: (Input, Endpoint[WeatherOperation, Input, Err, Output, StreamedInput, StreamedOutput])
}

object WeatherOperation {

  object reified extends WeatherGen[WeatherOperation] {
    def getCurrentTime() = GetCurrentTime()
    def getCity(cityId: CityId) = GetCity(GetCityInput(cityId))
    def getForecast(cityId: CityId) = GetForecast(GetForecastInput(cityId))
    def listCities(nextToken: Option[String] = None, pageSize: Option[Int] = None) = ListCities(ListCitiesInput(nextToken, pageSize))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: WeatherGen[P], f: PolyFunction5[P, P1]) extends WeatherGen[P1] {
    def getCurrentTime() = f[Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing](alg.getCurrentTime())
    def getCity(cityId: CityId) = f[GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing](alg.getCity(cityId))
    def getForecast(cityId: CityId) = f[GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing](alg.getForecast(cityId))
    def listCities(nextToken: Option[String] = None, pageSize: Option[Int] = None) = f[ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing](alg.listCities(nextToken, pageSize))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: WeatherGen[P]): PolyFunction5[WeatherOperation, P] = new PolyFunction5[WeatherOperation, P] {
    def apply[I, E, O, SI, SO](op: WeatherOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class GetCurrentTime() extends WeatherOperation[Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: WeatherGen[F]): F[Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing] = impl.getCurrentTime()
    def endpoint: (Unit, smithy4s.Endpoint[WeatherOperation,Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing]) = ((), GetCurrentTime)
  }
  object GetCurrentTime extends smithy4s.Endpoint[WeatherOperation,Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetCurrentTime")
    val input: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GetCurrentTimeOutput] = GetCurrentTimeOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Readonly(),
    )
    def wrap(input: Unit) = GetCurrentTime()
    override val errorable: Option[Nothing] = None
  }
  final case class GetCity(input: GetCityInput) extends WeatherOperation[GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: WeatherGen[F]): F[GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing] = impl.getCity(input.cityId)
    def endpoint: (GetCityInput, smithy4s.Endpoint[WeatherOperation,GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing]) = (input, GetCity)
  }
  object GetCity extends smithy4s.Endpoint[WeatherOperation,GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing] with Errorable[GetCityError] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetCity")
    val input: Schema[GetCityInput] = GetCityInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GetCityOutput] = GetCityOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Readonly(),
    )
    def wrap(input: GetCityInput) = GetCity(input)
    override val errorable: Option[Errorable[GetCityError]] = Some(this)
    val error: UnionSchema[GetCityError] = GetCityError.schema
    def liftError(throwable: Throwable): Option[GetCityError] = throwable match {
      case e: NoSuchResource => Some(GetCityError.NoSuchResourceCase(e))
      case _ => None
    }
    def unliftError(e: GetCityError): Throwable = e match {
      case GetCityError.NoSuchResourceCase(e) => e
    }
  }
  sealed trait GetCityError extends scala.Product with scala.Serializable {
    @inline final def widen: GetCityError = this
  }
  object GetCityError extends ShapeTag.Companion[GetCityError] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetCityError")

    val hints: Hints = Hints.empty

    final case class NoSuchResourceCase(noSuchResource: NoSuchResource) extends GetCityError
    def noSuchResource(noSuchResource:NoSuchResource): GetCityError = NoSuchResourceCase(noSuchResource)

    object NoSuchResourceCase {
      val hints: Hints = Hints.empty
      val schema: Schema[NoSuchResourceCase] = bijection(NoSuchResource.schema.addHints(hints), NoSuchResourceCase(_), _.noSuchResource)
      val alt = schema.oneOf[GetCityError]("NoSuchResource")
    }

    implicit val schema: UnionSchema[GetCityError] = union(
      NoSuchResourceCase.alt,
    ){
      case c: NoSuchResourceCase => NoSuchResourceCase.alt(c)
    }
  }
  final case class GetForecast(input: GetForecastInput) extends WeatherOperation[GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: WeatherGen[F]): F[GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing] = impl.getForecast(input.cityId)
    def endpoint: (GetForecastInput, smithy4s.Endpoint[WeatherOperation,GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing]) = (input, GetForecast)
  }
  object GetForecast extends smithy4s.Endpoint[WeatherOperation,GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetForecast")
    val input: Schema[GetForecastInput] = GetForecastInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GetForecastOutput] = GetForecastOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Readonly(),
    )
    def wrap(input: GetForecastInput) = GetForecast(input)
    override val errorable: Option[Nothing] = None
  }
  final case class ListCities(input: ListCitiesInput) extends WeatherOperation[ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: WeatherGen[F]): F[ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing] = impl.listCities(input.nextToken, input.pageSize)
    def endpoint: (ListCitiesInput, smithy4s.Endpoint[WeatherOperation,ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing]) = (input, ListCities)
  }
  object ListCities extends smithy4s.Endpoint[WeatherOperation,ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "ListCities")
    val input: Schema[ListCitiesInput] = ListCitiesInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[ListCitiesOutput] = ListCitiesOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Paginated(inputToken = None, outputToken = None, items = Some(smithy.api.NonEmptyString("items")), pageSize = None),
      smithy.api.Readonly(),
    )
    def wrap(input: ListCitiesInput) = ListCities(input)
    override val errorable: Option[Nothing] = None
  }
}

