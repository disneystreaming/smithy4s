package smithy4s.example

import smithy4s.Document
import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Timestamp
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.OperationSchema
import smithy4s.schema.Schema.unit

/** Just a dummy service to ensure that the rendered services compile
  * when testing core
  */
trait DummyServiceGen[F[_, _, _, _, _]] {
  self =>

  def dummy(str: Option[String] = None, int: Option[Int] = None, ts1: Option[Timestamp] = None, ts2: Option[Timestamp] = None, ts3: Option[Timestamp] = None, ts4: Option[Timestamp] = None, b: Option[Boolean] = None, sl: Option[List[String]] = None, ie: Option[Numbers] = None, on: Option[OpenNums] = None, ons: Option[OpenNumsStr] = None, slm: Option[Map[String, String]] = None): F[Queries, Nothing, Unit, Nothing, Nothing]
  def dummyHostPrefix(label1: String, label2: String, label3: HostLabelEnum): F[HostLabelInput, Nothing, Unit, Nothing, Nothing]
  def dummyPath(str: String, int: Int, ts1: Timestamp, ts2: Timestamp, ts3: Timestamp, ts4: Timestamp, b: Boolean, ie: Numbers): F[PathParams, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[DummyServiceGen[F]] = Transformation.of[DummyServiceGen[F]](this)
}

object DummyServiceGen extends Service.Mixin[DummyServiceGen, DummyServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "DummyService")
  val version: String = "0.0"

  val hints: Hints = Hints(
    ShapeId("smithy.api", "documentation") -> Document.fromString("Just a dummy service to ensure that the rendered services compile\nwhen testing core"),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[DummyServiceOperation, _, _, _, _, _]] = Vector(
    DummyServiceOperation.Dummy,
    DummyServiceOperation.DummyHostPrefix,
    DummyServiceOperation.DummyPath,
  )

  def input[I, E, O, SI, SO](op: DummyServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: DummyServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: DummyServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends DummyServiceOperation.Transformed[DummyServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: DummyServiceGen[DummyServiceOperation] = DummyServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: DummyServiceGen[P], f: PolyFunction5[P, P1]): DummyServiceGen[P1] = new DummyServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[DummyServiceOperation, P]): DummyServiceGen[P] = new DummyServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: DummyServiceGen[P]): PolyFunction5[DummyServiceOperation, P] = DummyServiceOperation.toPolyFunction(impl)

}

sealed trait DummyServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: DummyServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[DummyServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object DummyServiceOperation {

  object reified extends DummyServiceGen[DummyServiceOperation] {
    def dummy(str: Option[String] = None, int: Option[Int] = None, ts1: Option[Timestamp] = None, ts2: Option[Timestamp] = None, ts3: Option[Timestamp] = None, ts4: Option[Timestamp] = None, b: Option[Boolean] = None, sl: Option[List[String]] = None, ie: Option[Numbers] = None, on: Option[OpenNums] = None, ons: Option[OpenNumsStr] = None, slm: Option[Map[String, String]] = None): Dummy = Dummy(Queries(str, int, ts1, ts2, ts3, ts4, b, sl, ie, on, ons, slm))
    def dummyHostPrefix(label1: String, label2: String, label3: HostLabelEnum): DummyHostPrefix = DummyHostPrefix(HostLabelInput(label1, label2, label3))
    def dummyPath(str: String, int: Int, ts1: Timestamp, ts2: Timestamp, ts3: Timestamp, ts4: Timestamp, b: Boolean, ie: Numbers): DummyPath = DummyPath(PathParams(str, int, ts1, ts2, ts3, ts4, b, ie))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: DummyServiceGen[P], f: PolyFunction5[P, P1]) extends DummyServiceGen[P1] {
    def dummy(str: Option[String] = None, int: Option[Int] = None, ts1: Option[Timestamp] = None, ts2: Option[Timestamp] = None, ts3: Option[Timestamp] = None, ts4: Option[Timestamp] = None, b: Option[Boolean] = None, sl: Option[List[String]] = None, ie: Option[Numbers] = None, on: Option[OpenNums] = None, ons: Option[OpenNumsStr] = None, slm: Option[Map[String, String]] = None): P1[Queries, Nothing, Unit, Nothing, Nothing] = f[Queries, Nothing, Unit, Nothing, Nothing](alg.dummy(str, int, ts1, ts2, ts3, ts4, b, sl, ie, on, ons, slm))
    def dummyHostPrefix(label1: String, label2: String, label3: HostLabelEnum): P1[HostLabelInput, Nothing, Unit, Nothing, Nothing] = f[HostLabelInput, Nothing, Unit, Nothing, Nothing](alg.dummyHostPrefix(label1, label2, label3))
    def dummyPath(str: String, int: Int, ts1: Timestamp, ts2: Timestamp, ts3: Timestamp, ts4: Timestamp, b: Boolean, ie: Numbers): P1[PathParams, Nothing, Unit, Nothing, Nothing] = f[PathParams, Nothing, Unit, Nothing, Nothing](alg.dummyPath(str, int, ts1, ts2, ts3, ts4, b, ie))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: DummyServiceGen[P]): PolyFunction5[DummyServiceOperation, P] = new PolyFunction5[DummyServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: DummyServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class Dummy(input: Queries) extends DummyServiceOperation[Queries, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: DummyServiceGen[F]): F[Queries, Nothing, Unit, Nothing, Nothing] = impl.dummy(input.str, input.int, input.ts1, input.ts2, input.ts3, input.ts4, input.b, input.sl, input.ie, input.on, input.ons, input.slm)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[DummyServiceOperation,Queries, Nothing, Unit, Nothing, Nothing] = Dummy
  }
  object Dummy extends smithy4s.Endpoint[DummyServiceOperation,Queries, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Queries, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Dummy"))
      .withInput(Queries.schema)
      .withOutput(unit)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/dummy"), code = 200), smithy.api.Readonly())
    def wrap(input: Queries): Dummy = Dummy(input)
  }
  final case class DummyHostPrefix(input: HostLabelInput) extends DummyServiceOperation[HostLabelInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: DummyServiceGen[F]): F[HostLabelInput, Nothing, Unit, Nothing, Nothing] = impl.dummyHostPrefix(input.label1, input.label2, input.label3)
    def ordinal: Int = 1
    def endpoint: smithy4s.Endpoint[DummyServiceOperation,HostLabelInput, Nothing, Unit, Nothing, Nothing] = DummyHostPrefix
  }
  object DummyHostPrefix extends smithy4s.Endpoint[DummyServiceOperation,HostLabelInput, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[HostLabelInput, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "DummyHostPrefix"))
      .withInput(HostLabelInput.schema)
      .withOutput(unit)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/dummy"), code = 200), smithy.api.Endpoint(hostPrefix = smithy.api.NonEmptyString("foo.{label1}--abc{label2}.{label3}.secure.")))
    def wrap(input: HostLabelInput): DummyHostPrefix = DummyHostPrefix(input)
  }
  final case class DummyPath(input: PathParams) extends DummyServiceOperation[PathParams, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: DummyServiceGen[F]): F[PathParams, Nothing, Unit, Nothing, Nothing] = impl.dummyPath(input.str, input.int, input.ts1, input.ts2, input.ts3, input.ts4, input.b, input.ie)
    def ordinal: Int = 2
    def endpoint: smithy4s.Endpoint[DummyServiceOperation,PathParams, Nothing, Unit, Nothing, Nothing] = DummyPath
  }
  object DummyPath extends smithy4s.Endpoint[DummyServiceOperation,PathParams, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[PathParams, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "DummyPath"))
      .withInput(PathParams.schema)
      .withOutput(unit)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/dummy-path/{str}/{int}/{ts1}/{ts2}/{ts3}/{ts4}/{b}/{ie}?value=foo&baz=bar"), code = 200), smithy.api.Readonly())
    def wrap(input: PathParams): DummyPath = DummyPath(input)
  }
}

