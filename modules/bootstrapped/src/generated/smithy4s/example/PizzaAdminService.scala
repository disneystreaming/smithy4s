package smithy4s.example

import alloy.SimpleRestJson
import smithy.api.Http
import smithy.api.NonEmptyString
import smithy.api.Readonly
import smithy4s.Bijection
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

trait PizzaAdminServiceGen[F[_, _, _, _, _]] {
  self =>

  def addMenuItem(restaurant: String, menuItem: MenuItem): F[AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing]
  def getMenu(restaurant: String): F[GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing]
  def version(): F[Unit, Nothing, VersionOutput, Nothing, Nothing]
  def health(query: Option[String] = None): F[HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing]
  def headerEndpoint(uppercaseHeader: Option[String] = None, capitalizedHeader: Option[String] = None, lowercaseHeader: Option[String] = None, mixedHeader: Option[String] = None): F[HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing]
  def roundTrip(label: String, header: Option[String] = None, query: Option[String] = None, body: Option[String] = None): F[RoundTripData, Nothing, RoundTripData, Nothing, Nothing]
  def getEnum(aa: TheEnum): F[GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing]
  def getIntEnum(aa: EnumResult): F[GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing]
  def customCode(code: Int): F[CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing]
  def reservation(name: String, town: Option[String] = None): F[ReservationInput, Nothing, ReservationOutput, Nothing, Nothing]
  def echo(pathParam: String, body: EchoBody, queryParam: Option[String] = None): F[EchoInput, Nothing, Unit, Nothing, Nothing]
  def optionalOutput(): F[Unit, Nothing, OptionalOutputOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[PizzaAdminServiceGen[F]] = Transformation.of[PizzaAdminServiceGen[F]](this)
}

object PizzaAdminServiceGen extends Service.Mixin[PizzaAdminServiceGen, PizzaAdminServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "PizzaAdminService")
  val version: String = "1.0.0"

  val hints: Hints =
  Hints(
    SimpleRestJson(),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[PizzaAdminServiceOperation, _, _, _, _, _]] = Vector(
    PizzaAdminServiceOperation.AddMenuItem,
    PizzaAdminServiceOperation.GetMenu,
    PizzaAdminServiceOperation.Version,
    PizzaAdminServiceOperation.Health,
    PizzaAdminServiceOperation.HeaderEndpoint,
    PizzaAdminServiceOperation.RoundTrip,
    PizzaAdminServiceOperation.GetEnum,
    PizzaAdminServiceOperation.GetIntEnum,
    PizzaAdminServiceOperation.CustomCode,
    PizzaAdminServiceOperation.Reservation,
    PizzaAdminServiceOperation.Echo,
    PizzaAdminServiceOperation.OptionalOutput,
  )

  def input[I, E, O, SI, SO](op: PizzaAdminServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: PizzaAdminServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: PizzaAdminServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends PizzaAdminServiceOperation.Transformed[PizzaAdminServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: PizzaAdminServiceGen[PizzaAdminServiceOperation] = PizzaAdminServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: PizzaAdminServiceGen[P], f: PolyFunction5[P, P1]): PizzaAdminServiceGen[P1] = new PizzaAdminServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[PizzaAdminServiceOperation, P]): PizzaAdminServiceGen[P] = new PizzaAdminServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: PizzaAdminServiceGen[P]): PolyFunction5[PizzaAdminServiceOperation, P] = PizzaAdminServiceOperation.toPolyFunction(impl)

  type AddMenuItemError = PizzaAdminServiceOperation.AddMenuItemError
  val AddMenuItemError = PizzaAdminServiceOperation.AddMenuItemError
  type GetMenuError = PizzaAdminServiceOperation.GetMenuError
  val GetMenuError = PizzaAdminServiceOperation.GetMenuError
  type HealthError = PizzaAdminServiceOperation.HealthError
  val HealthError = PizzaAdminServiceOperation.HealthError
  type GetEnumError = PizzaAdminServiceOperation.GetEnumError
  val GetEnumError = PizzaAdminServiceOperation.GetEnumError
  type GetIntEnumError = PizzaAdminServiceOperation.GetIntEnumError
  val GetIntEnumError = PizzaAdminServiceOperation.GetIntEnumError
  type CustomCodeError = PizzaAdminServiceOperation.CustomCodeError
  val CustomCodeError = PizzaAdminServiceOperation.CustomCodeError
}

sealed trait PizzaAdminServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[PizzaAdminServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object PizzaAdminServiceOperation {

  object reified extends PizzaAdminServiceGen[PizzaAdminServiceOperation] {
    def addMenuItem(restaurant: String, menuItem: MenuItem) = AddMenuItem(AddMenuItemRequest(restaurant, menuItem))
    def getMenu(restaurant: String) = GetMenu(GetMenuRequest(restaurant))
    def version() = Version()
    def health(query: Option[String] = None) = Health(HealthRequest(query))
    def headerEndpoint(uppercaseHeader: Option[String] = None, capitalizedHeader: Option[String] = None, lowercaseHeader: Option[String] = None, mixedHeader: Option[String] = None) = HeaderEndpoint(HeaderEndpointData(uppercaseHeader, capitalizedHeader, lowercaseHeader, mixedHeader))
    def roundTrip(label: String, header: Option[String] = None, query: Option[String] = None, body: Option[String] = None) = RoundTrip(RoundTripData(label, header, query, body))
    def getEnum(aa: TheEnum) = GetEnum(GetEnumInput(aa))
    def getIntEnum(aa: EnumResult) = GetIntEnum(GetIntEnumInput(aa))
    def customCode(code: Int) = CustomCode(CustomCodeInput(code))
    def reservation(name: String, town: Option[String] = None) = Reservation(ReservationInput(name, town))
    def echo(pathParam: String, body: EchoBody, queryParam: Option[String] = None) = Echo(EchoInput(pathParam, body, queryParam))
    def optionalOutput() = OptionalOutput()
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: PizzaAdminServiceGen[P], f: PolyFunction5[P, P1]) extends PizzaAdminServiceGen[P1] {
    def addMenuItem(restaurant: String, menuItem: MenuItem) = f[AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing](alg.addMenuItem(restaurant, menuItem))
    def getMenu(restaurant: String) = f[GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing](alg.getMenu(restaurant))
    def version() = f[Unit, Nothing, VersionOutput, Nothing, Nothing](alg.version())
    def health(query: Option[String] = None) = f[HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing](alg.health(query))
    def headerEndpoint(uppercaseHeader: Option[String] = None, capitalizedHeader: Option[String] = None, lowercaseHeader: Option[String] = None, mixedHeader: Option[String] = None) = f[HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing](alg.headerEndpoint(uppercaseHeader, capitalizedHeader, lowercaseHeader, mixedHeader))
    def roundTrip(label: String, header: Option[String] = None, query: Option[String] = None, body: Option[String] = None) = f[RoundTripData, Nothing, RoundTripData, Nothing, Nothing](alg.roundTrip(label, header, query, body))
    def getEnum(aa: TheEnum) = f[GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing](alg.getEnum(aa))
    def getIntEnum(aa: EnumResult) = f[GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing](alg.getIntEnum(aa))
    def customCode(code: Int) = f[CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing](alg.customCode(code))
    def reservation(name: String, town: Option[String] = None) = f[ReservationInput, Nothing, ReservationOutput, Nothing, Nothing](alg.reservation(name, town))
    def echo(pathParam: String, body: EchoBody, queryParam: Option[String] = None) = f[EchoInput, Nothing, Unit, Nothing, Nothing](alg.echo(pathParam, body, queryParam))
    def optionalOutput() = f[Unit, Nothing, OptionalOutputOutput, Nothing, Nothing](alg.optionalOutput())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: PizzaAdminServiceGen[P]): PolyFunction5[PizzaAdminServiceOperation, P] = new PolyFunction5[PizzaAdminServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: PizzaAdminServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class AddMenuItem(input: AddMenuItemRequest) extends PizzaAdminServiceOperation[AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing] = impl.addMenuItem(input.restaurant, input.menuItem)
    def ordinal = 0
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing] = AddMenuItem
  }
  object AddMenuItem extends smithy4s.Endpoint[PizzaAdminServiceOperation,AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing] with Errorable[AddMenuItemError] {
    val id: ShapeId = ShapeId("smithy4s.example", "AddMenuItem")
    val input: Schema[AddMenuItemRequest] = AddMenuItemRequest.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[AddMenuItemResult] = AddMenuItemResult.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("POST"), uri = NonEmptyString("/restaurant/{restaurant}/menu/item"), code = 201),
    )
    def wrap(input: AddMenuItemRequest) = AddMenuItem(input)
    override val errorable: Option[Errorable[AddMenuItemError]] = Some(this)
    val error: UnionSchema[AddMenuItemError] = AddMenuItemError.schema
    def liftError(throwable: Throwable): Option[AddMenuItemError] = throwable match {
      case e: smithy4s.example.PriceError => Some(AddMenuItemError.PriceErrorCase(e))
      case e: smithy4s.example.GenericServerError => Some(AddMenuItemError.GenericServerErrorCase(e))
      case e: smithy4s.example.GenericClientError => Some(AddMenuItemError.GenericClientErrorCase(e))
      case _ => None
    }
    def unliftError(e: AddMenuItemError): Throwable = e match {
      case AddMenuItemError.PriceErrorCase(e) => e
      case AddMenuItemError.GenericServerErrorCase(e) => e
      case AddMenuItemError.GenericClientErrorCase(e) => e
    }
  }
  sealed trait AddMenuItemError extends scala.Product with scala.Serializable {
    @inline final def widen: AddMenuItemError = this
    def _ordinal: Int
  }
  object AddMenuItemError extends ShapeTag.Companion[AddMenuItemError] {
    final case class PriceErrorCase(priceError: smithy4s.example.PriceError) extends AddMenuItemError { final def _ordinal: Int = 0 }
    final case class GenericServerErrorCase(genericServerError: smithy4s.example.GenericServerError) extends AddMenuItemError { final def _ordinal: Int = 1 }
    final case class GenericClientErrorCase(genericClientError: smithy4s.example.GenericClientError) extends AddMenuItemError { final def _ordinal: Int = 2 }

    object PriceErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.PriceError, PriceErrorCase] = Bijection(PriceErrorCase(_), _.priceError)
      implicit val toValue: Bijection[PriceErrorCase, smithy4s.example.PriceError] = fromValue.swap
      val schema: Schema[PriceErrorCase] = bijection(smithy4s.example.PriceError.schema, fromValue)
    }
    object GenericServerErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.GenericServerError, GenericServerErrorCase] = Bijection(GenericServerErrorCase(_), _.genericServerError)
      implicit val toValue: Bijection[GenericServerErrorCase, smithy4s.example.GenericServerError] = fromValue.swap
      val schema: Schema[GenericServerErrorCase] = bijection(smithy4s.example.GenericServerError.schema, fromValue)
    }
    object GenericClientErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.GenericClientError, GenericClientErrorCase] = Bijection(GenericClientErrorCase(_), _.genericClientError)
      implicit val toValue: Bijection[GenericClientErrorCase, smithy4s.example.GenericClientError] = fromValue.swap
      val schema: Schema[GenericClientErrorCase] = bijection(smithy4s.example.GenericClientError.schema, fromValue)
    }

    val PriceError = PriceErrorCase.schema.oneOf[AddMenuItemError]("PriceError")
    val GenericServerError = GenericServerErrorCase.schema.oneOf[AddMenuItemError]("GenericServerError")
    val GenericClientError = GenericClientErrorCase.schema.oneOf[AddMenuItemError]("GenericClientError")

    implicit val schema: UnionSchema[AddMenuItemError] = union(
      PriceError,
      GenericServerError,
      GenericClientError,
    ){
      _._ordinal
    }
    
  }
  final case class GetMenu(input: GetMenuRequest) extends PizzaAdminServiceOperation[GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing] = impl.getMenu(input.restaurant)
    def ordinal = 1
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing] = GetMenu
  }
  object GetMenu extends smithy4s.Endpoint[PizzaAdminServiceOperation,GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing] with Errorable[GetMenuError] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetMenu")
    val input: Schema[GetMenuRequest] = GetMenuRequest.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GetMenuResult] = GetMenuResult.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("GET"), uri = NonEmptyString("/restaurant/{restaurant}/menu"), code = 200),
      Readonly(),
    )
    def wrap(input: GetMenuRequest) = GetMenu(input)
    override val errorable: Option[Errorable[GetMenuError]] = Some(this)
    val error: UnionSchema[GetMenuError] = GetMenuError.schema
    def liftError(throwable: Throwable): Option[GetMenuError] = throwable match {
      case e: smithy4s.example.GenericClientError => Some(GetMenuError.GenericClientErrorCase(e))
      case e: smithy4s.example.FallbackError2 => Some(GetMenuError.FallbackError2Case(e))
      case e: smithy4s.example.FallbackError => Some(GetMenuError.FallbackErrorCase(e))
      case e: smithy4s.example.NotFoundError => Some(GetMenuError.NotFoundErrorCase(e))
      case _ => None
    }
    def unliftError(e: GetMenuError): Throwable = e match {
      case GetMenuError.GenericClientErrorCase(e) => e
      case GetMenuError.FallbackError2Case(e) => e
      case GetMenuError.FallbackErrorCase(e) => e
      case GetMenuError.NotFoundErrorCase(e) => e
    }
  }
  sealed trait GetMenuError extends scala.Product with scala.Serializable {
    @inline final def widen: GetMenuError = this
    def _ordinal: Int
  }
  object GetMenuError extends ShapeTag.Companion[GetMenuError] {
    final case class GenericClientErrorCase(genericClientError: smithy4s.example.GenericClientError) extends GetMenuError { final def _ordinal: Int = 0 }
    final case class FallbackError2Case(fallbackError2: smithy4s.example.FallbackError2) extends GetMenuError { final def _ordinal: Int = 1 }
    final case class FallbackErrorCase(fallbackError: smithy4s.example.FallbackError) extends GetMenuError { final def _ordinal: Int = 2 }
    final case class NotFoundErrorCase(notFoundError: smithy4s.example.NotFoundError) extends GetMenuError { final def _ordinal: Int = 3 }

    object GenericClientErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.GenericClientError, GenericClientErrorCase] = Bijection(GenericClientErrorCase(_), _.genericClientError)
      implicit val toValue: Bijection[GenericClientErrorCase, smithy4s.example.GenericClientError] = fromValue.swap
      val schema: Schema[GenericClientErrorCase] = bijection(smithy4s.example.GenericClientError.schema, fromValue)
    }
    object FallbackError2Case {
      implicit val fromValue: Bijection[smithy4s.example.FallbackError2, FallbackError2Case] = Bijection(FallbackError2Case(_), _.fallbackError2)
      implicit val toValue: Bijection[FallbackError2Case, smithy4s.example.FallbackError2] = fromValue.swap
      val schema: Schema[FallbackError2Case] = bijection(smithy4s.example.FallbackError2.schema, fromValue)
    }
    object FallbackErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.FallbackError, FallbackErrorCase] = Bijection(FallbackErrorCase(_), _.fallbackError)
      implicit val toValue: Bijection[FallbackErrorCase, smithy4s.example.FallbackError] = fromValue.swap
      val schema: Schema[FallbackErrorCase] = bijection(smithy4s.example.FallbackError.schema, fromValue)
    }
    object NotFoundErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.NotFoundError, NotFoundErrorCase] = Bijection(NotFoundErrorCase(_), _.notFoundError)
      implicit val toValue: Bijection[NotFoundErrorCase, smithy4s.example.NotFoundError] = fromValue.swap
      val schema: Schema[NotFoundErrorCase] = bijection(smithy4s.example.NotFoundError.schema, fromValue)
    }

    val GenericClientError = GenericClientErrorCase.schema.oneOf[GetMenuError]("GenericClientError")
    val FallbackError2 = FallbackError2Case.schema.oneOf[GetMenuError]("FallbackError2")
    val FallbackError = FallbackErrorCase.schema.oneOf[GetMenuError]("FallbackError")
    val NotFoundError = NotFoundErrorCase.schema.oneOf[GetMenuError]("NotFoundError")

    implicit val schema: UnionSchema[GetMenuError] = union(
      GenericClientError,
      FallbackError2,
      FallbackError,
      NotFoundError,
    ){
      _._ordinal
    }
    
  }
  final case class Version() extends PizzaAdminServiceOperation[Unit, Nothing, VersionOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[Unit, Nothing, VersionOutput, Nothing, Nothing] = impl.version()
    def ordinal = 2
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,Unit, Nothing, VersionOutput, Nothing, Nothing] = Version
  }
  object Version extends smithy4s.Endpoint[PizzaAdminServiceOperation,Unit, Nothing, VersionOutput, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "Version")
    val input: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[VersionOutput] = VersionOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("GET"), uri = NonEmptyString("/version"), code = 200),
      Readonly(),
    )
    def wrap(input: Unit) = Version()
    override val errorable: Option[Nothing] = None
  }
  final case class Health(input: HealthRequest) extends PizzaAdminServiceOperation[HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing] = impl.health(input.query)
    def ordinal = 3
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing] = Health
  }
  object Health extends smithy4s.Endpoint[PizzaAdminServiceOperation,HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing] with Errorable[HealthError] {
    val id: ShapeId = ShapeId("smithy4s.example", "Health")
    val input: Schema[HealthRequest] = HealthRequest.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[HealthResponse] = HealthResponse.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("GET"), uri = NonEmptyString("/health"), code = 200),
      Readonly(),
    )
    def wrap(input: HealthRequest) = Health(input)
    override val errorable: Option[Errorable[HealthError]] = Some(this)
    val error: UnionSchema[HealthError] = HealthError.schema
    def liftError(throwable: Throwable): Option[HealthError] = throwable match {
      case e: smithy4s.example.UnknownServerError => Some(HealthError.UnknownServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: HealthError): Throwable = e match {
      case HealthError.UnknownServerErrorCase(e) => e
    }
  }
  sealed trait HealthError extends scala.Product with scala.Serializable {
    @inline final def widen: HealthError = this
    def _ordinal: Int
  }
  object HealthError extends ShapeTag.Companion[HealthError] {
    final case class UnknownServerErrorCase(unknownServerError: smithy4s.example.UnknownServerError) extends HealthError { final def _ordinal: Int = 0 }

    object UnknownServerErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.UnknownServerError, UnknownServerErrorCase] = Bijection(UnknownServerErrorCase(_), _.unknownServerError)
      implicit val toValue: Bijection[UnknownServerErrorCase, smithy4s.example.UnknownServerError] = fromValue.swap
      val schema: Schema[UnknownServerErrorCase] = bijection(smithy4s.example.UnknownServerError.schema, fromValue)
    }

    val UnknownServerError = UnknownServerErrorCase.schema.oneOf[HealthError]("UnknownServerError")

    implicit val schema: UnionSchema[HealthError] = union(
      UnknownServerError,
    ){
      _._ordinal
    }
    
  }
  final case class HeaderEndpoint(input: HeaderEndpointData) extends PizzaAdminServiceOperation[HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing] = impl.headerEndpoint(input.uppercaseHeader, input.capitalizedHeader, input.lowercaseHeader, input.mixedHeader)
    def ordinal = 4
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing] = HeaderEndpoint
  }
  object HeaderEndpoint extends smithy4s.Endpoint[PizzaAdminServiceOperation,HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "HeaderEndpoint")
    val input: Schema[HeaderEndpointData] = HeaderEndpointData.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[HeaderEndpointData] = HeaderEndpointData.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("POST"), uri = NonEmptyString("/headers/"), code = 200),
    )
    def wrap(input: HeaderEndpointData) = HeaderEndpoint(input)
    override val errorable: Option[Nothing] = None
  }
  final case class RoundTrip(input: RoundTripData) extends PizzaAdminServiceOperation[RoundTripData, Nothing, RoundTripData, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[RoundTripData, Nothing, RoundTripData, Nothing, Nothing] = impl.roundTrip(input.label, input.header, input.query, input.body)
    def ordinal = 5
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,RoundTripData, Nothing, RoundTripData, Nothing, Nothing] = RoundTrip
  }
  object RoundTrip extends smithy4s.Endpoint[PizzaAdminServiceOperation,RoundTripData, Nothing, RoundTripData, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "RoundTrip")
    val input: Schema[RoundTripData] = RoundTripData.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[RoundTripData] = RoundTripData.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("POST"), uri = NonEmptyString("/roundTrip/{label}"), code = 200),
    )
    def wrap(input: RoundTripData) = RoundTrip(input)
    override val errorable: Option[Nothing] = None
  }
  final case class GetEnum(input: GetEnumInput) extends PizzaAdminServiceOperation[GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing] = impl.getEnum(input.aa)
    def ordinal = 6
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing] = GetEnum
  }
  object GetEnum extends smithy4s.Endpoint[PizzaAdminServiceOperation,GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing] with Errorable[GetEnumError] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetEnum")
    val input: Schema[GetEnumInput] = GetEnumInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GetEnumOutput] = GetEnumOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("GET"), uri = NonEmptyString("/get-enum/{aa}"), code = 200),
      Readonly(),
    )
    def wrap(input: GetEnumInput) = GetEnum(input)
    override val errorable: Option[Errorable[GetEnumError]] = Some(this)
    val error: UnionSchema[GetEnumError] = GetEnumError.schema
    def liftError(throwable: Throwable): Option[GetEnumError] = throwable match {
      case e: smithy4s.example.UnknownServerError => Some(GetEnumError.UnknownServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: GetEnumError): Throwable = e match {
      case GetEnumError.UnknownServerErrorCase(e) => e
    }
  }
  sealed trait GetEnumError extends scala.Product with scala.Serializable {
    @inline final def widen: GetEnumError = this
    def _ordinal: Int
  }
  object GetEnumError extends ShapeTag.Companion[GetEnumError] {
    final case class UnknownServerErrorCase(unknownServerError: smithy4s.example.UnknownServerError) extends GetEnumError { final def _ordinal: Int = 0 }

    object UnknownServerErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.UnknownServerError, UnknownServerErrorCase] = Bijection(UnknownServerErrorCase(_), _.unknownServerError)
      implicit val toValue: Bijection[UnknownServerErrorCase, smithy4s.example.UnknownServerError] = fromValue.swap
      val schema: Schema[UnknownServerErrorCase] = bijection(smithy4s.example.UnknownServerError.schema, fromValue)
    }

    val UnknownServerError = UnknownServerErrorCase.schema.oneOf[GetEnumError]("UnknownServerError")

    implicit val schema: UnionSchema[GetEnumError] = union(
      UnknownServerError,
    ){
      _._ordinal
    }
    
  }
  final case class GetIntEnum(input: GetIntEnumInput) extends PizzaAdminServiceOperation[GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing] = impl.getIntEnum(input.aa)
    def ordinal = 7
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing] = GetIntEnum
  }
  object GetIntEnum extends smithy4s.Endpoint[PizzaAdminServiceOperation,GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing] with Errorable[GetIntEnumError] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetIntEnum")
    val input: Schema[GetIntEnumInput] = GetIntEnumInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[GetIntEnumOutput] = GetIntEnumOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("GET"), uri = NonEmptyString("/get-int-enum/{aa}"), code = 200),
      Readonly(),
    )
    def wrap(input: GetIntEnumInput) = GetIntEnum(input)
    override val errorable: Option[Errorable[GetIntEnumError]] = Some(this)
    val error: UnionSchema[GetIntEnumError] = GetIntEnumError.schema
    def liftError(throwable: Throwable): Option[GetIntEnumError] = throwable match {
      case e: smithy4s.example.UnknownServerError => Some(GetIntEnumError.UnknownServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: GetIntEnumError): Throwable = e match {
      case GetIntEnumError.UnknownServerErrorCase(e) => e
    }
  }
  sealed trait GetIntEnumError extends scala.Product with scala.Serializable {
    @inline final def widen: GetIntEnumError = this
    def _ordinal: Int
  }
  object GetIntEnumError extends ShapeTag.Companion[GetIntEnumError] {
    final case class UnknownServerErrorCase(unknownServerError: smithy4s.example.UnknownServerError) extends GetIntEnumError { final def _ordinal: Int = 0 }

    object UnknownServerErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.UnknownServerError, UnknownServerErrorCase] = Bijection(UnknownServerErrorCase(_), _.unknownServerError)
      implicit val toValue: Bijection[UnknownServerErrorCase, smithy4s.example.UnknownServerError] = fromValue.swap
      val schema: Schema[UnknownServerErrorCase] = bijection(smithy4s.example.UnknownServerError.schema, fromValue)
    }

    val UnknownServerError = UnknownServerErrorCase.schema.oneOf[GetIntEnumError]("UnknownServerError")

    implicit val schema: UnionSchema[GetIntEnumError] = union(
      UnknownServerError,
    ){
      _._ordinal
    }
    
  }
  final case class CustomCode(input: CustomCodeInput) extends PizzaAdminServiceOperation[CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing] = impl.customCode(input.code)
    def ordinal = 8
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing] = CustomCode
  }
  object CustomCode extends smithy4s.Endpoint[PizzaAdminServiceOperation,CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing] with Errorable[CustomCodeError] {
    val id: ShapeId = ShapeId("smithy4s.example", "CustomCode")
    val input: Schema[CustomCodeInput] = CustomCodeInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[CustomCodeOutput] = CustomCodeOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("GET"), uri = NonEmptyString("/custom-code/{code}"), code = 200),
      Readonly(),
    )
    def wrap(input: CustomCodeInput) = CustomCode(input)
    override val errorable: Option[Errorable[CustomCodeError]] = Some(this)
    val error: UnionSchema[CustomCodeError] = CustomCodeError.schema
    def liftError(throwable: Throwable): Option[CustomCodeError] = throwable match {
      case e: smithy4s.example.UnknownServerError => Some(CustomCodeError.UnknownServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: CustomCodeError): Throwable = e match {
      case CustomCodeError.UnknownServerErrorCase(e) => e
    }
  }
  sealed trait CustomCodeError extends scala.Product with scala.Serializable {
    @inline final def widen: CustomCodeError = this
    def _ordinal: Int
  }
  object CustomCodeError extends ShapeTag.Companion[CustomCodeError] {
    final case class UnknownServerErrorCase(unknownServerError: smithy4s.example.UnknownServerError) extends CustomCodeError { final def _ordinal: Int = 0 }

    object UnknownServerErrorCase {
      implicit val fromValue: Bijection[smithy4s.example.UnknownServerError, UnknownServerErrorCase] = Bijection(UnknownServerErrorCase(_), _.unknownServerError)
      implicit val toValue: Bijection[UnknownServerErrorCase, smithy4s.example.UnknownServerError] = fromValue.swap
      val schema: Schema[UnknownServerErrorCase] = bijection(smithy4s.example.UnknownServerError.schema, fromValue)
    }

    val UnknownServerError = UnknownServerErrorCase.schema.oneOf[CustomCodeError]("UnknownServerError")

    implicit val schema: UnionSchema[CustomCodeError] = union(
      UnknownServerError,
    ){
      _._ordinal
    }
    
  }
  final case class Reservation(input: ReservationInput) extends PizzaAdminServiceOperation[ReservationInput, Nothing, ReservationOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[ReservationInput, Nothing, ReservationOutput, Nothing, Nothing] = impl.reservation(input.name, input.town)
    def ordinal = 9
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,ReservationInput, Nothing, ReservationOutput, Nothing, Nothing] = Reservation
  }
  object Reservation extends smithy4s.Endpoint[PizzaAdminServiceOperation,ReservationInput, Nothing, ReservationOutput, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "Reservation")
    val input: Schema[ReservationInput] = ReservationInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[ReservationOutput] = ReservationOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("POST"), uri = NonEmptyString("/book/{name}"), code = 200),
    )
    def wrap(input: ReservationInput) = Reservation(input)
    override val errorable: Option[Nothing] = None
  }
  final case class Echo(input: EchoInput) extends PizzaAdminServiceOperation[EchoInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[EchoInput, Nothing, Unit, Nothing, Nothing] = impl.echo(input.pathParam, input.body, input.queryParam)
    def ordinal = 10
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,EchoInput, Nothing, Unit, Nothing, Nothing] = Echo
  }
  object Echo extends smithy4s.Endpoint[PizzaAdminServiceOperation,EchoInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "Echo")
    val input: Schema[EchoInput] = EchoInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("POST"), uri = NonEmptyString("/echo/{pathParam}"), code = 200),
    )
    def wrap(input: EchoInput) = Echo(input)
    override val errorable: Option[Nothing] = None
  }
  final case class OptionalOutput() extends PizzaAdminServiceOperation[Unit, Nothing, OptionalOutputOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[Unit, Nothing, OptionalOutputOutput, Nothing, Nothing] = impl.optionalOutput()
    def ordinal = 11
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,Unit, Nothing, OptionalOutputOutput, Nothing, Nothing] = OptionalOutput
  }
  object OptionalOutput extends smithy4s.Endpoint[PizzaAdminServiceOperation,Unit, Nothing, OptionalOutputOutput, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example", "OptionalOutput")
    val input: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[OptionalOutputOutput] = OptionalOutputOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints =
    Hints(
      Http(method = NonEmptyString("GET"), uri = NonEmptyString("/optional-output"), code = 200),
      Readonly(),
    )
    def wrap(input: Unit) = OptionalOutput()
    override val errorable: Option[Nothing] = None
  }
}

