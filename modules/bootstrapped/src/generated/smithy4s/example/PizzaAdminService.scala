package smithy4s.example

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
  def headRequest(): F[Unit, Nothing, HeadRequestOutput, Nothing, Nothing]
  def noContentRequest(): F[Unit, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[PizzaAdminServiceGen[F]] = Transformation.of[PizzaAdminServiceGen[F]](this)
}

object PizzaAdminServiceGen extends Service.Mixin[PizzaAdminServiceGen, PizzaAdminServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "PizzaAdminService")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
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
    PizzaAdminServiceOperation.HeadRequest,
    PizzaAdminServiceOperation.NoContentRequest,
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
    def addMenuItem(restaurant: String, menuItem: MenuItem): AddMenuItem = AddMenuItem(AddMenuItemRequest(restaurant, menuItem))
    def getMenu(restaurant: String): GetMenu = GetMenu(GetMenuRequest(restaurant))
    def version(): Version = Version()
    def health(query: Option[String] = None): Health = Health(HealthRequest(query))
    def headerEndpoint(uppercaseHeader: Option[String] = None, capitalizedHeader: Option[String] = None, lowercaseHeader: Option[String] = None, mixedHeader: Option[String] = None): HeaderEndpoint = HeaderEndpoint(HeaderEndpointData(uppercaseHeader, capitalizedHeader, lowercaseHeader, mixedHeader))
    def roundTrip(label: String, header: Option[String] = None, query: Option[String] = None, body: Option[String] = None): RoundTrip = RoundTrip(RoundTripData(label, header, query, body))
    def getEnum(aa: TheEnum): GetEnum = GetEnum(GetEnumInput(aa))
    def getIntEnum(aa: EnumResult): GetIntEnum = GetIntEnum(GetIntEnumInput(aa))
    def customCode(code: Int): CustomCode = CustomCode(CustomCodeInput(code))
    def reservation(name: String, town: Option[String] = None): Reservation = Reservation(ReservationInput(name, town))
    def echo(pathParam: String, body: EchoBody, queryParam: Option[String] = None): Echo = Echo(EchoInput(pathParam, body, queryParam))
    def optionalOutput(): OptionalOutput = OptionalOutput()
    def headRequest(): HeadRequest = HeadRequest()
    def noContentRequest(): NoContentRequest = NoContentRequest()
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: PizzaAdminServiceGen[P], f: PolyFunction5[P, P1]) extends PizzaAdminServiceGen[P1] {
    def addMenuItem(restaurant: String, menuItem: MenuItem): P1[AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing] = f[AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing](alg.addMenuItem(restaurant, menuItem))
    def getMenu(restaurant: String): P1[GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing] = f[GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing](alg.getMenu(restaurant))
    def version(): P1[Unit, Nothing, VersionOutput, Nothing, Nothing] = f[Unit, Nothing, VersionOutput, Nothing, Nothing](alg.version())
    def health(query: Option[String] = None): P1[HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing] = f[HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing](alg.health(query))
    def headerEndpoint(uppercaseHeader: Option[String] = None, capitalizedHeader: Option[String] = None, lowercaseHeader: Option[String] = None, mixedHeader: Option[String] = None): P1[HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing] = f[HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing](alg.headerEndpoint(uppercaseHeader, capitalizedHeader, lowercaseHeader, mixedHeader))
    def roundTrip(label: String, header: Option[String] = None, query: Option[String] = None, body: Option[String] = None): P1[RoundTripData, Nothing, RoundTripData, Nothing, Nothing] = f[RoundTripData, Nothing, RoundTripData, Nothing, Nothing](alg.roundTrip(label, header, query, body))
    def getEnum(aa: TheEnum): P1[GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing] = f[GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing](alg.getEnum(aa))
    def getIntEnum(aa: EnumResult): P1[GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing] = f[GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing](alg.getIntEnum(aa))
    def customCode(code: Int): P1[CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing] = f[CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing](alg.customCode(code))
    def reservation(name: String, town: Option[String] = None): P1[ReservationInput, Nothing, ReservationOutput, Nothing, Nothing] = f[ReservationInput, Nothing, ReservationOutput, Nothing, Nothing](alg.reservation(name, town))
    def echo(pathParam: String, body: EchoBody, queryParam: Option[String] = None): P1[EchoInput, Nothing, Unit, Nothing, Nothing] = f[EchoInput, Nothing, Unit, Nothing, Nothing](alg.echo(pathParam, body, queryParam))
    def optionalOutput(): P1[Unit, Nothing, OptionalOutputOutput, Nothing, Nothing] = f[Unit, Nothing, OptionalOutputOutput, Nothing, Nothing](alg.optionalOutput())
    def headRequest(): P1[Unit, Nothing, HeadRequestOutput, Nothing, Nothing] = f[Unit, Nothing, HeadRequestOutput, Nothing, Nothing](alg.headRequest())
    def noContentRequest(): P1[Unit, Nothing, Unit, Nothing, Nothing] = f[Unit, Nothing, Unit, Nothing, Nothing](alg.noContentRequest())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: PizzaAdminServiceGen[P]): PolyFunction5[PizzaAdminServiceOperation, P] = new PolyFunction5[PizzaAdminServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: PizzaAdminServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class AddMenuItem(input: AddMenuItemRequest) extends PizzaAdminServiceOperation[AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing] = impl.addMenuItem(input.restaurant, input.menuItem)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing] = AddMenuItem
  }
  object AddMenuItem extends smithy4s.Endpoint[PizzaAdminServiceOperation,AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing] {
    val schema: OperationSchema[AddMenuItemRequest, PizzaAdminServiceOperation.AddMenuItemError, AddMenuItemResult, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "AddMenuItem"))
      .withInput(AddMenuItemRequest.schema)
      .withError(AddMenuItemError.errorSchema)
      .withOutput(AddMenuItemResult.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/restaurant/{restaurant}/menu/item"), code = 201))
    def wrap(input: AddMenuItemRequest): AddMenuItem = AddMenuItem(input)
  }
  sealed trait AddMenuItemError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: AddMenuItemError = this
    def $ordinal: Int

    object project {
      def priceError: Option[PriceError] = AddMenuItemError.PriceErrorCase.alt.project.lift(self).map(_.priceError)
      def genericServerError: Option[GenericServerError] = AddMenuItemError.GenericServerErrorCase.alt.project.lift(self).map(_.genericServerError)
      def genericClientError: Option[GenericClientError] = AddMenuItemError.GenericClientErrorCase.alt.project.lift(self).map(_.genericClientError)
    }

    def accept[A](visitor: AddMenuItemError.Visitor[A]): A = this match {
      case value: AddMenuItemError.PriceErrorCase => visitor.priceError(value.priceError)
      case value: AddMenuItemError.GenericServerErrorCase => visitor.genericServerError(value.genericServerError)
      case value: AddMenuItemError.GenericClientErrorCase => visitor.genericClientError(value.genericClientError)
    }
  }
  object AddMenuItemError extends ErrorSchema.Companion[AddMenuItemError] {

    def priceError(priceError: PriceError): AddMenuItemError = PriceErrorCase(priceError)
    def genericServerError(genericServerError: GenericServerError): AddMenuItemError = GenericServerErrorCase(genericServerError)
    def genericClientError(genericClientError: GenericClientError): AddMenuItemError = GenericClientErrorCase(genericClientError)

    val id: ShapeId = ShapeId("smithy4s.example", "AddMenuItemError")

    val hints: Hints = Hints.empty

    final case class PriceErrorCase(priceError: PriceError) extends AddMenuItemError { final def $ordinal: Int = 0 }
    final case class GenericServerErrorCase(genericServerError: GenericServerError) extends AddMenuItemError { final def $ordinal: Int = 1 }
    final case class GenericClientErrorCase(genericClientError: GenericClientError) extends AddMenuItemError { final def $ordinal: Int = 2 }

    object PriceErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[AddMenuItemError.PriceErrorCase] = bijection(PriceError.schema.addHints(hints), AddMenuItemError.PriceErrorCase(_), _.priceError)
      val alt = schema.oneOf[AddMenuItemError]("PriceError")
    }
    object GenericServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[AddMenuItemError.GenericServerErrorCase] = bijection(GenericServerError.schema.addHints(hints), AddMenuItemError.GenericServerErrorCase(_), _.genericServerError)
      val alt = schema.oneOf[AddMenuItemError]("GenericServerError")
    }
    object GenericClientErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[AddMenuItemError.GenericClientErrorCase] = bijection(GenericClientError.schema.addHints(hints), AddMenuItemError.GenericClientErrorCase(_), _.genericClientError)
      val alt = schema.oneOf[AddMenuItemError]("GenericClientError")
    }

    trait Visitor[A] {
      def priceError(value: PriceError): A
      def genericServerError(value: GenericServerError): A
      def genericClientError(value: GenericClientError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def priceError(value: PriceError): A = default
        def genericServerError(value: GenericServerError): A = default
        def genericClientError(value: GenericClientError): A = default
      }
    }

    implicit val schema: Schema[AddMenuItemError] = union(
      AddMenuItemError.PriceErrorCase.alt,
      AddMenuItemError.GenericServerErrorCase.alt,
      AddMenuItemError.GenericClientErrorCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[AddMenuItemError] = throwable match {
      case e: PriceError => Some(AddMenuItemError.PriceErrorCase(e))
      case e: GenericServerError => Some(AddMenuItemError.GenericServerErrorCase(e))
      case e: GenericClientError => Some(AddMenuItemError.GenericClientErrorCase(e))
      case _ => None
    }
    def unliftError(e: AddMenuItemError): Throwable = e match {
      case AddMenuItemError.PriceErrorCase(e) => e
      case AddMenuItemError.GenericServerErrorCase(e) => e
      case AddMenuItemError.GenericClientErrorCase(e) => e
    }
  }
  final case class GetMenu(input: GetMenuRequest) extends PizzaAdminServiceOperation[GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing] = impl.getMenu(input.restaurant)
    def ordinal: Int = 1
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing] = GetMenu
  }
  object GetMenu extends smithy4s.Endpoint[PizzaAdminServiceOperation,GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing] {
    val schema: OperationSchema[GetMenuRequest, PizzaAdminServiceOperation.GetMenuError, GetMenuResult, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GetMenu"))
      .withInput(GetMenuRequest.schema)
      .withError(GetMenuError.errorSchema)
      .withOutput(GetMenuResult.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/restaurant/{restaurant}/menu"), code = 200), smithy.api.Readonly())
    def wrap(input: GetMenuRequest): GetMenu = GetMenu(input)
  }
  sealed trait GetMenuError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: GetMenuError = this
    def $ordinal: Int

    object project {
      def genericClientError: Option[GenericClientError] = GetMenuError.GenericClientErrorCase.alt.project.lift(self).map(_.genericClientError)
      def fallbackError2: Option[FallbackError2] = GetMenuError.FallbackError2Case.alt.project.lift(self).map(_.fallbackError2)
      def fallbackError: Option[FallbackError] = GetMenuError.FallbackErrorCase.alt.project.lift(self).map(_.fallbackError)
      def notFoundError: Option[NotFoundError] = GetMenuError.NotFoundErrorCase.alt.project.lift(self).map(_.notFoundError)
    }

    def accept[A](visitor: GetMenuError.Visitor[A]): A = this match {
      case value: GetMenuError.GenericClientErrorCase => visitor.genericClientError(value.genericClientError)
      case value: GetMenuError.FallbackError2Case => visitor.fallbackError2(value.fallbackError2)
      case value: GetMenuError.FallbackErrorCase => visitor.fallbackError(value.fallbackError)
      case value: GetMenuError.NotFoundErrorCase => visitor.notFoundError(value.notFoundError)
    }
  }
  object GetMenuError extends ErrorSchema.Companion[GetMenuError] {

    def genericClientError(genericClientError: GenericClientError): GetMenuError = GenericClientErrorCase(genericClientError)
    def fallbackError2(fallbackError2: FallbackError2): GetMenuError = FallbackError2Case(fallbackError2)
    def fallbackError(fallbackError: FallbackError): GetMenuError = FallbackErrorCase(fallbackError)
    def notFoundError(notFoundError: NotFoundError): GetMenuError = NotFoundErrorCase(notFoundError)

    val id: ShapeId = ShapeId("smithy4s.example", "GetMenuError")

    val hints: Hints = Hints.empty

    final case class GenericClientErrorCase(genericClientError: GenericClientError) extends GetMenuError { final def $ordinal: Int = 0 }
    final case class FallbackError2Case(fallbackError2: FallbackError2) extends GetMenuError { final def $ordinal: Int = 1 }
    final case class FallbackErrorCase(fallbackError: FallbackError) extends GetMenuError { final def $ordinal: Int = 2 }
    final case class NotFoundErrorCase(notFoundError: NotFoundError) extends GetMenuError { final def $ordinal: Int = 3 }

    object GenericClientErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GetMenuError.GenericClientErrorCase] = bijection(GenericClientError.schema.addHints(hints), GetMenuError.GenericClientErrorCase(_), _.genericClientError)
      val alt = schema.oneOf[GetMenuError]("GenericClientError")
    }
    object FallbackError2Case {
      val hints: Hints = Hints.empty
      val schema: Schema[GetMenuError.FallbackError2Case] = bijection(FallbackError2.schema.addHints(hints), GetMenuError.FallbackError2Case(_), _.fallbackError2)
      val alt = schema.oneOf[GetMenuError]("FallbackError2")
    }
    object FallbackErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GetMenuError.FallbackErrorCase] = bijection(FallbackError.schema.addHints(hints), GetMenuError.FallbackErrorCase(_), _.fallbackError)
      val alt = schema.oneOf[GetMenuError]("FallbackError")
    }
    object NotFoundErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GetMenuError.NotFoundErrorCase] = bijection(NotFoundError.schema.addHints(hints), GetMenuError.NotFoundErrorCase(_), _.notFoundError)
      val alt = schema.oneOf[GetMenuError]("NotFoundError")
    }

    trait Visitor[A] {
      def genericClientError(value: GenericClientError): A
      def fallbackError2(value: FallbackError2): A
      def fallbackError(value: FallbackError): A
      def notFoundError(value: NotFoundError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def genericClientError(value: GenericClientError): A = default
        def fallbackError2(value: FallbackError2): A = default
        def fallbackError(value: FallbackError): A = default
        def notFoundError(value: NotFoundError): A = default
      }
    }

    implicit val schema: Schema[GetMenuError] = union(
      GetMenuError.GenericClientErrorCase.alt,
      GetMenuError.FallbackError2Case.alt,
      GetMenuError.FallbackErrorCase.alt,
      GetMenuError.NotFoundErrorCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[GetMenuError] = throwable match {
      case e: GenericClientError => Some(GetMenuError.GenericClientErrorCase(e))
      case e: FallbackError2 => Some(GetMenuError.FallbackError2Case(e))
      case e: FallbackError => Some(GetMenuError.FallbackErrorCase(e))
      case e: NotFoundError => Some(GetMenuError.NotFoundErrorCase(e))
      case _ => None
    }
    def unliftError(e: GetMenuError): Throwable = e match {
      case GetMenuError.GenericClientErrorCase(e) => e
      case GetMenuError.FallbackError2Case(e) => e
      case GetMenuError.FallbackErrorCase(e) => e
      case GetMenuError.NotFoundErrorCase(e) => e
    }
  }
  final case class Version() extends PizzaAdminServiceOperation[Unit, Nothing, VersionOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[Unit, Nothing, VersionOutput, Nothing, Nothing] = impl.version()
    def ordinal: Int = 2
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,Unit, Nothing, VersionOutput, Nothing, Nothing] = Version
  }
  object Version extends smithy4s.Endpoint[PizzaAdminServiceOperation,Unit, Nothing, VersionOutput, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, VersionOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Version"))
      .withInput(unit)
      .withOutput(VersionOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/version"), code = 200), smithy.api.Readonly())
    def wrap(input: Unit): Version = Version()
  }
  final case class Health(input: HealthRequest) extends PizzaAdminServiceOperation[HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing] = impl.health(input.query)
    def ordinal: Int = 3
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing] = Health
  }
  object Health extends smithy4s.Endpoint[PizzaAdminServiceOperation,HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing] {
    val schema: OperationSchema[HealthRequest, PizzaAdminServiceOperation.HealthError, HealthResponse, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Health"))
      .withInput(HealthRequest.schema)
      .withError(HealthError.errorSchema)
      .withOutput(HealthResponse.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/health"), code = 200), smithy.api.Readonly())
    def wrap(input: HealthRequest): Health = Health(input)
  }
  sealed trait HealthError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: HealthError = this
    def $ordinal: Int

    object project {
      def unknownServerError: Option[UnknownServerError] = HealthError.UnknownServerErrorCase.alt.project.lift(self).map(_.unknownServerError)
    }

    def accept[A](visitor: HealthError.Visitor[A]): A = this match {
      case value: HealthError.UnknownServerErrorCase => visitor.unknownServerError(value.unknownServerError)
    }
  }
  object HealthError extends ErrorSchema.Companion[HealthError] {

    def unknownServerError(unknownServerError: UnknownServerError): HealthError = UnknownServerErrorCase(unknownServerError)

    val id: ShapeId = ShapeId("smithy4s.example", "HealthError")

    val hints: Hints = Hints.empty

    final case class UnknownServerErrorCase(unknownServerError: UnknownServerError) extends HealthError { final def $ordinal: Int = 0 }

    object UnknownServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[HealthError.UnknownServerErrorCase] = bijection(UnknownServerError.schema.addHints(hints), HealthError.UnknownServerErrorCase(_), _.unknownServerError)
      val alt = schema.oneOf[HealthError]("UnknownServerError")
    }

    trait Visitor[A] {
      def unknownServerError(value: UnknownServerError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def unknownServerError(value: UnknownServerError): A = default
      }
    }

    implicit val schema: Schema[HealthError] = union(
      HealthError.UnknownServerErrorCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[HealthError] = throwable match {
      case e: UnknownServerError => Some(HealthError.UnknownServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: HealthError): Throwable = e match {
      case HealthError.UnknownServerErrorCase(e) => e
    }
  }
  final case class HeaderEndpoint(input: HeaderEndpointData) extends PizzaAdminServiceOperation[HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing] = impl.headerEndpoint(input.uppercaseHeader, input.capitalizedHeader, input.lowercaseHeader, input.mixedHeader)
    def ordinal: Int = 4
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing] = HeaderEndpoint
  }
  object HeaderEndpoint extends smithy4s.Endpoint[PizzaAdminServiceOperation,HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing] {
    val schema: OperationSchema[HeaderEndpointData, Nothing, HeaderEndpointData, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "HeaderEndpoint"))
      .withInput(HeaderEndpointData.schema)
      .withOutput(HeaderEndpointData.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/headers/"), code = 200))
    def wrap(input: HeaderEndpointData): HeaderEndpoint = HeaderEndpoint(input)
  }
  final case class RoundTrip(input: RoundTripData) extends PizzaAdminServiceOperation[RoundTripData, Nothing, RoundTripData, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[RoundTripData, Nothing, RoundTripData, Nothing, Nothing] = impl.roundTrip(input.label, input.header, input.query, input.body)
    def ordinal: Int = 5
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,RoundTripData, Nothing, RoundTripData, Nothing, Nothing] = RoundTrip
  }
  object RoundTrip extends smithy4s.Endpoint[PizzaAdminServiceOperation,RoundTripData, Nothing, RoundTripData, Nothing, Nothing] {
    val schema: OperationSchema[RoundTripData, Nothing, RoundTripData, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "RoundTrip"))
      .withInput(RoundTripData.schema)
      .withOutput(RoundTripData.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/roundTrip/{label}"), code = 200))
    def wrap(input: RoundTripData): RoundTrip = RoundTrip(input)
  }
  final case class GetEnum(input: GetEnumInput) extends PizzaAdminServiceOperation[GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing] = impl.getEnum(input.aa)
    def ordinal: Int = 6
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing] = GetEnum
  }
  object GetEnum extends smithy4s.Endpoint[PizzaAdminServiceOperation,GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing] {
    val schema: OperationSchema[GetEnumInput, PizzaAdminServiceOperation.GetEnumError, GetEnumOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GetEnum"))
      .withInput(GetEnumInput.schema)
      .withError(GetEnumError.errorSchema)
      .withOutput(GetEnumOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/get-enum/{aa}"), code = 200), smithy.api.Readonly())
    def wrap(input: GetEnumInput): GetEnum = GetEnum(input)
  }
  sealed trait GetEnumError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: GetEnumError = this
    def $ordinal: Int

    object project {
      def unknownServerError: Option[UnknownServerError] = GetEnumError.UnknownServerErrorCase.alt.project.lift(self).map(_.unknownServerError)
    }

    def accept[A](visitor: GetEnumError.Visitor[A]): A = this match {
      case value: GetEnumError.UnknownServerErrorCase => visitor.unknownServerError(value.unknownServerError)
    }
  }
  object GetEnumError extends ErrorSchema.Companion[GetEnumError] {

    def unknownServerError(unknownServerError: UnknownServerError): GetEnumError = UnknownServerErrorCase(unknownServerError)

    val id: ShapeId = ShapeId("smithy4s.example", "GetEnumError")

    val hints: Hints = Hints.empty

    final case class UnknownServerErrorCase(unknownServerError: UnknownServerError) extends GetEnumError { final def $ordinal: Int = 0 }

    object UnknownServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GetEnumError.UnknownServerErrorCase] = bijection(UnknownServerError.schema.addHints(hints), GetEnumError.UnknownServerErrorCase(_), _.unknownServerError)
      val alt = schema.oneOf[GetEnumError]("UnknownServerError")
    }

    trait Visitor[A] {
      def unknownServerError(value: UnknownServerError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def unknownServerError(value: UnknownServerError): A = default
      }
    }

    implicit val schema: Schema[GetEnumError] = union(
      GetEnumError.UnknownServerErrorCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[GetEnumError] = throwable match {
      case e: UnknownServerError => Some(GetEnumError.UnknownServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: GetEnumError): Throwable = e match {
      case GetEnumError.UnknownServerErrorCase(e) => e
    }
  }
  final case class GetIntEnum(input: GetIntEnumInput) extends PizzaAdminServiceOperation[GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing] = impl.getIntEnum(input.aa)
    def ordinal: Int = 7
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing] = GetIntEnum
  }
  object GetIntEnum extends smithy4s.Endpoint[PizzaAdminServiceOperation,GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing] {
    val schema: OperationSchema[GetIntEnumInput, PizzaAdminServiceOperation.GetIntEnumError, GetIntEnumOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GetIntEnum"))
      .withInput(GetIntEnumInput.schema)
      .withError(GetIntEnumError.errorSchema)
      .withOutput(GetIntEnumOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/get-int-enum/{aa}"), code = 200), smithy.api.Readonly())
    def wrap(input: GetIntEnumInput): GetIntEnum = GetIntEnum(input)
  }
  sealed trait GetIntEnumError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: GetIntEnumError = this
    def $ordinal: Int

    object project {
      def unknownServerError: Option[UnknownServerError] = GetIntEnumError.UnknownServerErrorCase.alt.project.lift(self).map(_.unknownServerError)
    }

    def accept[A](visitor: GetIntEnumError.Visitor[A]): A = this match {
      case value: GetIntEnumError.UnknownServerErrorCase => visitor.unknownServerError(value.unknownServerError)
    }
  }
  object GetIntEnumError extends ErrorSchema.Companion[GetIntEnumError] {

    def unknownServerError(unknownServerError: UnknownServerError): GetIntEnumError = UnknownServerErrorCase(unknownServerError)

    val id: ShapeId = ShapeId("smithy4s.example", "GetIntEnumError")

    val hints: Hints = Hints.empty

    final case class UnknownServerErrorCase(unknownServerError: UnknownServerError) extends GetIntEnumError { final def $ordinal: Int = 0 }

    object UnknownServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GetIntEnumError.UnknownServerErrorCase] = bijection(UnknownServerError.schema.addHints(hints), GetIntEnumError.UnknownServerErrorCase(_), _.unknownServerError)
      val alt = schema.oneOf[GetIntEnumError]("UnknownServerError")
    }

    trait Visitor[A] {
      def unknownServerError(value: UnknownServerError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def unknownServerError(value: UnknownServerError): A = default
      }
    }

    implicit val schema: Schema[GetIntEnumError] = union(
      GetIntEnumError.UnknownServerErrorCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[GetIntEnumError] = throwable match {
      case e: UnknownServerError => Some(GetIntEnumError.UnknownServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: GetIntEnumError): Throwable = e match {
      case GetIntEnumError.UnknownServerErrorCase(e) => e
    }
  }
  final case class CustomCode(input: CustomCodeInput) extends PizzaAdminServiceOperation[CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing] = impl.customCode(input.code)
    def ordinal: Int = 8
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing] = CustomCode
  }
  object CustomCode extends smithy4s.Endpoint[PizzaAdminServiceOperation,CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing] {
    val schema: OperationSchema[CustomCodeInput, PizzaAdminServiceOperation.CustomCodeError, CustomCodeOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "CustomCode"))
      .withInput(CustomCodeInput.schema)
      .withError(CustomCodeError.errorSchema)
      .withOutput(CustomCodeOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/custom-code/{code}"), code = 200), smithy.api.Readonly())
    def wrap(input: CustomCodeInput): CustomCode = CustomCode(input)
  }
  sealed trait CustomCodeError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: CustomCodeError = this
    def $ordinal: Int

    object project {
      def unknownServerError: Option[UnknownServerError] = CustomCodeError.UnknownServerErrorCase.alt.project.lift(self).map(_.unknownServerError)
    }

    def accept[A](visitor: CustomCodeError.Visitor[A]): A = this match {
      case value: CustomCodeError.UnknownServerErrorCase => visitor.unknownServerError(value.unknownServerError)
    }
  }
  object CustomCodeError extends ErrorSchema.Companion[CustomCodeError] {

    def unknownServerError(unknownServerError: UnknownServerError): CustomCodeError = UnknownServerErrorCase(unknownServerError)

    val id: ShapeId = ShapeId("smithy4s.example", "CustomCodeError")

    val hints: Hints = Hints.empty

    final case class UnknownServerErrorCase(unknownServerError: UnknownServerError) extends CustomCodeError { final def $ordinal: Int = 0 }

    object UnknownServerErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[CustomCodeError.UnknownServerErrorCase] = bijection(UnknownServerError.schema.addHints(hints), CustomCodeError.UnknownServerErrorCase(_), _.unknownServerError)
      val alt = schema.oneOf[CustomCodeError]("UnknownServerError")
    }

    trait Visitor[A] {
      def unknownServerError(value: UnknownServerError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def unknownServerError(value: UnknownServerError): A = default
      }
    }

    implicit val schema: Schema[CustomCodeError] = union(
      CustomCodeError.UnknownServerErrorCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[CustomCodeError] = throwable match {
      case e: UnknownServerError => Some(CustomCodeError.UnknownServerErrorCase(e))
      case _ => None
    }
    def unliftError(e: CustomCodeError): Throwable = e match {
      case CustomCodeError.UnknownServerErrorCase(e) => e
    }
  }
  final case class Reservation(input: ReservationInput) extends PizzaAdminServiceOperation[ReservationInput, Nothing, ReservationOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[ReservationInput, Nothing, ReservationOutput, Nothing, Nothing] = impl.reservation(input.name, input.town)
    def ordinal: Int = 9
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,ReservationInput, Nothing, ReservationOutput, Nothing, Nothing] = Reservation
  }
  object Reservation extends smithy4s.Endpoint[PizzaAdminServiceOperation,ReservationInput, Nothing, ReservationOutput, Nothing, Nothing] {
    val schema: OperationSchema[ReservationInput, Nothing, ReservationOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Reservation"))
      .withInput(ReservationInput.schema)
      .withOutput(ReservationOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/book/{name}"), code = 200))
    def wrap(input: ReservationInput): Reservation = Reservation(input)
  }
  final case class Echo(input: EchoInput) extends PizzaAdminServiceOperation[EchoInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[EchoInput, Nothing, Unit, Nothing, Nothing] = impl.echo(input.pathParam, input.body, input.queryParam)
    def ordinal: Int = 10
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,EchoInput, Nothing, Unit, Nothing, Nothing] = Echo
  }
  object Echo extends smithy4s.Endpoint[PizzaAdminServiceOperation,EchoInput, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[EchoInput, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Echo"))
      .withInput(EchoInput.schema)
      .withOutput(unit)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/echo/{pathParam}"), code = 200))
    def wrap(input: EchoInput): Echo = Echo(input)
  }
  final case class OptionalOutput() extends PizzaAdminServiceOperation[Unit, Nothing, OptionalOutputOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[Unit, Nothing, OptionalOutputOutput, Nothing, Nothing] = impl.optionalOutput()
    def ordinal: Int = 11
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,Unit, Nothing, OptionalOutputOutput, Nothing, Nothing] = OptionalOutput
  }
  object OptionalOutput extends smithy4s.Endpoint[PizzaAdminServiceOperation,Unit, Nothing, OptionalOutputOutput, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, OptionalOutputOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "OptionalOutput"))
      .withInput(unit)
      .withOutput(OptionalOutputOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/optional-output"), code = 200), smithy.api.Readonly())
    def wrap(input: Unit): OptionalOutput = OptionalOutput()
  }
  final case class HeadRequest() extends PizzaAdminServiceOperation[Unit, Nothing, HeadRequestOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[Unit, Nothing, HeadRequestOutput, Nothing, Nothing] = impl.headRequest()
    def ordinal: Int = 12
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,Unit, Nothing, HeadRequestOutput, Nothing, Nothing] = HeadRequest
  }
  object HeadRequest extends smithy4s.Endpoint[PizzaAdminServiceOperation,Unit, Nothing, HeadRequestOutput, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, HeadRequestOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "HeadRequest"))
      .withInput(unit)
      .withOutput(HeadRequestOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("HEAD"), uri = smithy.api.NonEmptyString("/head-request"), code = 200), smithy.api.Readonly())
    def wrap(input: Unit): HeadRequest = HeadRequest()
  }
  final case class NoContentRequest() extends PizzaAdminServiceOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: PizzaAdminServiceGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl.noContentRequest()
    def ordinal: Int = 13
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[PizzaAdminServiceOperation,Unit, Nothing, Unit, Nothing, Nothing] = NoContentRequest
  }
  object NoContentRequest extends smithy4s.Endpoint[PizzaAdminServiceOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "NoContentRequest"))
      .withInput(unit)
      .withOutput(unit)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/no-content"), code = 204), smithy.api.Readonly())
    def wrap(input: Unit): NoContentRequest = NoContentRequest()
  }
}

