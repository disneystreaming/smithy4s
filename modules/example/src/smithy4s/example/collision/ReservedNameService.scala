package smithy4s.example.collision

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ServiceProduct
import smithy4s.ShapeId
import smithy4s.StreamingSchema
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.Schema.unit

trait ReservedNameServiceGen[F[_, _, _, _, _]] {
  self =>

  def set(set: Set[String]): F[SetInput, Nothing, Unit, Nothing, Nothing]
  def list(list: List[String]): F[ListInput, Nothing, Unit, Nothing, Nothing]
  def map(value: Map[String, String]): F[MapInput, Nothing, Unit, Nothing, Nothing]
  def option(value: Option[String] = None): F[OptionInput, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[ReservedNameServiceGen[F]] = Transformation.of[ReservedNameServiceGen[F]](this)
}

trait ReservedNameServiceProductGen[F[_, _, _, _, _]] {
  self =>

  def set: F[SetInput, Nothing, Unit, Nothing, Nothing]
  def list: F[ListInput, Nothing, Unit, Nothing, Nothing]
  def map: F[MapInput, Nothing, Unit, Nothing, Nothing]
  def option: F[OptionInput, Nothing, Unit, Nothing, Nothing]
}

object ReservedNameServiceGen extends Service.Mixin[ReservedNameServiceGen, ReservedNameServiceOperation] with ServiceProduct.Mirror[ReservedNameServiceGen] {

  val id: ShapeId = ShapeId("smithy4s.example.collision", "ReservedNameService")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: List[smithy4s.Endpoint[ReservedNameServiceOperation, _, _, _, _, _]] = List(
    ReservedNameServiceOperation._Set,
    ReservedNameServiceOperation._List,
    ReservedNameServiceOperation._Map,
    ReservedNameServiceOperation._Option,
  )

  def endpoint[I, E, O, SI, SO](op: ReservedNameServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends ReservedNameServiceOperation.Transformed[ReservedNameServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: ReservedNameServiceGen[ReservedNameServiceOperation] = ReservedNameServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: ReservedNameServiceGen[P], f: PolyFunction5[P, P1]): ReservedNameServiceGen[P1] = new ReservedNameServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[ReservedNameServiceOperation, P]): ReservedNameServiceGen[P] = new ReservedNameServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: ReservedNameServiceGen[P]): PolyFunction5[ReservedNameServiceOperation, P] = ReservedNameServiceOperation.toPolyFunction(impl)

  type Prod[F[_, _, _, _, _]] = ReservedNameServiceProductGen[F]
  val serviceProduct: ServiceProduct.Aux[ReservedNameServiceProductGen, ReservedNameServiceGen] = ReservedNameServiceProductGen
}

object ReservedNameServiceProductGen extends ServiceProduct[ReservedNameServiceProductGen] {
  type Alg[F[_, _, _, _, _]] = ReservedNameServiceGen[F]
  val service: ReservedNameServiceGen.type = ReservedNameServiceGen

  def endpointsProduct: ReservedNameServiceProductGen[service.Endpoint] = new ReservedNameServiceProductGen[service.Endpoint] {
    def set: service.Endpoint[SetInput, Nothing, Unit, Nothing, Nothing] = ReservedNameServiceOperation._Set
    def list: service.Endpoint[ListInput, Nothing, Unit, Nothing, Nothing] = ReservedNameServiceOperation._List
    def map: service.Endpoint[MapInput, Nothing, Unit, Nothing, Nothing] = ReservedNameServiceOperation._Map
    def option: service.Endpoint[OptionInput, Nothing, Unit, Nothing, Nothing] = ReservedNameServiceOperation._Option
  }

  def toPolyFunction[P2[_, _, _, _, _]](algebra: ReservedNameServiceProductGen[P2]) = new PolyFunction5[service.Endpoint, P2] {
    def apply[I, E, O, SI, SO](fa: service.Endpoint[I, E, O, SI, SO]): P2[I, E, O, SI, SO] =
    fa match {
      case ReservedNameServiceOperation._Set => algebra.set.asInstanceOf[P2[I, E, O, SI, SO]]
      case ReservedNameServiceOperation._List => algebra.list.asInstanceOf[P2[I, E, O, SI, SO]]
      case ReservedNameServiceOperation._Map => algebra.map.asInstanceOf[P2[I, E, O, SI, SO]]
      case ReservedNameServiceOperation._Option => algebra.option.asInstanceOf[P2[I, E, O, SI, SO]]
    }
  }

  def mapK5[F[_, _, _, _, _], G[_, _, _, _, _]](alg: ReservedNameServiceProductGen[F], f: PolyFunction5[F, G]): ReservedNameServiceProductGen[G] = {
    new ReservedNameServiceProductGen[G] {
      def set: G[SetInput, Nothing, Unit, Nothing, Nothing] = f[SetInput, Nothing, Unit, Nothing, Nothing](alg.set)
      def list: G[ListInput, Nothing, Unit, Nothing, Nothing] = f[ListInput, Nothing, Unit, Nothing, Nothing](alg.list)
      def map: G[MapInput, Nothing, Unit, Nothing, Nothing] = f[MapInput, Nothing, Unit, Nothing, Nothing](alg.map)
      def option: G[OptionInput, Nothing, Unit, Nothing, Nothing] = f[OptionInput, Nothing, Unit, Nothing, Nothing](alg.option)
    }
  }
}

sealed trait ReservedNameServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: ReservedNameServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def endpoint: (Input, Endpoint[ReservedNameServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput])
}

object ReservedNameServiceOperation {

  object reified extends ReservedNameServiceGen[ReservedNameServiceOperation] {
    def set(set: Set[String]) = _Set(SetInput(set))
    def list(list: List[String]) = _List(ListInput(list))
    def map(value: Map[String, String]) = _Map(MapInput(value))
    def option(value: Option[String] = None) = _Option(OptionInput(value))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: ReservedNameServiceGen[P], f: PolyFunction5[P, P1]) extends ReservedNameServiceGen[P1] {
    def set(set: Set[String]) = f[SetInput, Nothing, Unit, Nothing, Nothing](alg.set(set))
    def list(list: List[String]) = f[ListInput, Nothing, Unit, Nothing, Nothing](alg.list(list))
    def map(value: Map[String, String]) = f[MapInput, Nothing, Unit, Nothing, Nothing](alg.map(value))
    def option(value: Option[String] = None) = f[OptionInput, Nothing, Unit, Nothing, Nothing](alg.option(value))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: ReservedNameServiceGen[P]): PolyFunction5[ReservedNameServiceOperation, P] = new PolyFunction5[ReservedNameServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: ReservedNameServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class _Set(input: SetInput) extends ReservedNameServiceOperation[SetInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ReservedNameServiceGen[F]): F[SetInput, Nothing, Unit, Nothing, Nothing] = impl.set(input.set)
    def endpoint: (SetInput, smithy4s.Endpoint[ReservedNameServiceOperation,SetInput, Nothing, Unit, Nothing, Nothing]) = (input, _Set)
  }
  object _Set extends smithy4s.Endpoint[ReservedNameServiceOperation,SetInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "Set")
    val input: Schema[SetInput] = SetInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/set/"), code = 204),
    )
    def wrap(input: SetInput) = _Set(input)
    override val errorable: Option[Nothing] = None
  }
  final case class _List(input: ListInput) extends ReservedNameServiceOperation[ListInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ReservedNameServiceGen[F]): F[ListInput, Nothing, Unit, Nothing, Nothing] = impl.list(input.list)
    def endpoint: (ListInput, smithy4s.Endpoint[ReservedNameServiceOperation,ListInput, Nothing, Unit, Nothing, Nothing]) = (input, _List)
  }
  object _List extends smithy4s.Endpoint[ReservedNameServiceOperation,ListInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "List")
    val input: Schema[ListInput] = ListInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/list/"), code = 204),
    )
    def wrap(input: ListInput) = _List(input)
    override val errorable: Option[Nothing] = None
  }
  final case class _Map(input: MapInput) extends ReservedNameServiceOperation[MapInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ReservedNameServiceGen[F]): F[MapInput, Nothing, Unit, Nothing, Nothing] = impl.map(input.value)
    def endpoint: (MapInput, smithy4s.Endpoint[ReservedNameServiceOperation,MapInput, Nothing, Unit, Nothing, Nothing]) = (input, _Map)
  }
  object _Map extends smithy4s.Endpoint[ReservedNameServiceOperation,MapInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "Map")
    val input: Schema[MapInput] = MapInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/map/"), code = 204),
    )
    def wrap(input: MapInput) = _Map(input)
    override val errorable: Option[Nothing] = None
  }
  final case class _Option(input: OptionInput) extends ReservedNameServiceOperation[OptionInput, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: ReservedNameServiceGen[F]): F[OptionInput, Nothing, Unit, Nothing, Nothing] = impl.option(input.value)
    def endpoint: (OptionInput, smithy4s.Endpoint[ReservedNameServiceOperation,OptionInput, Nothing, Unit, Nothing, Nothing]) = (input, _Option)
  }
  object _Option extends smithy4s.Endpoint[ReservedNameServiceOperation,OptionInput, Nothing, Unit, Nothing, Nothing] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "Option")
    val input: Schema[OptionInput] = OptionInput.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints(
      smithy.api.Http(method = smithy.api.NonEmptyString("POST"), uri = smithy.api.NonEmptyString("/api/option/"), code = 204),
    )
    def wrap(input: OptionInput) = _Option(input)
    override val errorable: Option[Nothing] = None
  }
}

