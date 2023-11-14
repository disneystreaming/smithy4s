package smithy4s.example

import smithy4s.Document
import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.ErrorSchema
import smithy4s.schema.OperationSchema
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
    ShapeId("smithy.api", "documentation") -> Document.fromString("Provides weather forecasts."),
    smithy.api.Paginated(inputToken = Some(smithy.api.NonEmptyString("nextToken")), outputToken = Some(smithy.api.NonEmptyString("nextToken")), items = None, pageSize = Some(smithy.api.NonEmptyString("pageSize"))),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[WeatherOperation, _, _, _, _, _]] = Vector(
    WeatherOperation.GetCurrentTime,
    WeatherOperation.GetCity,
    WeatherOperation.GetForecast,
    WeatherOperation.ListCities,
  )

  def input[I, E, O, SI, SO](op: WeatherOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: WeatherOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: WeatherOperation[I, E, O, SI, SO]) = op.endpoint
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
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[WeatherOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object WeatherOperation {

  object reified extends WeatherGen[WeatherOperation] {
    def getCurrentTime(): GetCurrentTime = GetCurrentTime()
    def getCity(cityId: CityId): GetCity = GetCity(GetCityInput(cityId))
    def getForecast(cityId: CityId): GetForecast = GetForecast(GetForecastInput(cityId))
    def listCities(nextToken: Option[String] = None, pageSize: Option[Int] = None): ListCities = ListCities(ListCitiesInput(nextToken, pageSize))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: WeatherGen[P], f: PolyFunction5[P, P1]) extends WeatherGen[P1] {
    def getCurrentTime(): P1[Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing] = f[Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing](alg.getCurrentTime())
    def getCity(cityId: CityId): P1[GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing] = f[GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing](alg.getCity(cityId))
    def getForecast(cityId: CityId): P1[GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing] = f[GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing](alg.getForecast(cityId))
    def listCities(nextToken: Option[String] = None, pageSize: Option[Int] = None): P1[ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing] = f[ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing](alg.listCities(nextToken, pageSize))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: WeatherGen[P]): PolyFunction5[WeatherOperation, P] = new PolyFunction5[WeatherOperation, P] {
    def apply[I, E, O, SI, SO](op: WeatherOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class GetCurrentTime() extends WeatherOperation[Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: WeatherGen[F]): F[Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing] = impl.getCurrentTime()
    def ordinal: Int = 0
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[WeatherOperation,Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing] = GetCurrentTime
  }
  object GetCurrentTime extends smithy4s.Endpoint[WeatherOperation,Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, GetCurrentTimeOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GetCurrentTime"))
      .withInput(unit)
      .withOutput(GetCurrentTimeOutput.schema)
      .withHints(smithy.api.Readonly())
    def wrap(input: Unit): GetCurrentTime = GetCurrentTime()
  }
  final case class GetCity(input: GetCityInput) extends WeatherOperation[GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: WeatherGen[F]): F[GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing] = impl.getCity(input.cityId)
    def ordinal: Int = 1
    def endpoint: smithy4s.Endpoint[WeatherOperation,GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing] = GetCity
  }
  object GetCity extends smithy4s.Endpoint[WeatherOperation,GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing] {
    val schema: OperationSchema[GetCityInput, WeatherOperation.GetCityError, GetCityOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GetCity"))
      .withInput(GetCityInput.schema)
      .withError(GetCityError.errorSchema)
      .withOutput(GetCityOutput.schema)
      .withHints(smithy.api.Readonly())
    def wrap(input: GetCityInput): GetCity = GetCity(input)
  }
  sealed trait GetCityError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: GetCityError = this
    def $ordinal: Int

    object project {
      def noSuchResource: Option[NoSuchResource] = GetCityError.NoSuchResourceCase.alt.project.lift(self).map(_.noSuchResource)
    }

    def accept[A](visitor: GetCityError.Visitor[A]): A = this match {
      case value: GetCityError.NoSuchResourceCase => visitor.noSuchResource(value.noSuchResource)
    }
  }
  object GetCityError extends ErrorSchema.Companion[GetCityError] {

    def noSuchResource(noSuchResource: NoSuchResource): GetCityError = NoSuchResourceCase(noSuchResource)

    val id: ShapeId = ShapeId("smithy4s.example", "GetCityError")

    val hints: Hints = Hints.empty

    final case class NoSuchResourceCase(noSuchResource: NoSuchResource) extends GetCityError { final def $ordinal: Int = 0 }

    object NoSuchResourceCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GetCityError.NoSuchResourceCase] = bijection(NoSuchResource.schema.addHints(hints), GetCityError.NoSuchResourceCase(_), _.noSuchResource)
      val alt = schema.oneOf[GetCityError]("NoSuchResource")
    }

    trait Visitor[A] {
      def noSuchResource(value: NoSuchResource): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def noSuchResource(value: NoSuchResource): A = default
      }
    }

    implicit val schema: Schema[GetCityError] = union(
      GetCityError.NoSuchResourceCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[GetCityError] = throwable match {
      case e: NoSuchResource => Some(GetCityError.NoSuchResourceCase(e))
      case _ => None
    }
    def unliftError(e: GetCityError): Throwable = e match {
      case GetCityError.NoSuchResourceCase(e) => e
    }
  }
  final case class GetForecast(input: GetForecastInput) extends WeatherOperation[GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: WeatherGen[F]): F[GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing] = impl.getForecast(input.cityId)
    def ordinal: Int = 2
    def endpoint: smithy4s.Endpoint[WeatherOperation,GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing] = GetForecast
  }
  object GetForecast extends smithy4s.Endpoint[WeatherOperation,GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing] {
    val schema: OperationSchema[GetForecastInput, Nothing, GetForecastOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GetForecast"))
      .withInput(GetForecastInput.schema)
      .withOutput(GetForecastOutput.schema)
      .withHints(smithy.api.Readonly())
    def wrap(input: GetForecastInput): GetForecast = GetForecast(input)
  }
  final case class ListCities(input: ListCitiesInput) extends WeatherOperation[ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: WeatherGen[F]): F[ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing] = impl.listCities(input.nextToken, input.pageSize)
    def ordinal: Int = 3
    def endpoint: smithy4s.Endpoint[WeatherOperation,ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing] = ListCities
  }
  object ListCities extends smithy4s.Endpoint[WeatherOperation,ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing] {
    val schema: OperationSchema[ListCitiesInput, Nothing, ListCitiesOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "ListCities"))
      .withInput(ListCitiesInput.schema)
      .withOutput(ListCitiesOutput.schema)
      .withHints(smithy.api.Paginated(inputToken = None, outputToken = None, items = Some(smithy.api.NonEmptyString("items")), pageSize = None), smithy.api.Readonly())
    def wrap(input: ListCitiesInput): ListCities = ListCities(input)
  }
}

