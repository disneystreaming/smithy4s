package smithy4s.example

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.OperationSchema

trait CheckQueryServiceGen[F[_, _, _, _, _]] {
  self =>

  def checkQueryKindZVariantA(inp: Map[String, List[String]] = Map()): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing]
  def checkQueryKindYVariant(inp: Map[String, List[String]] = Map()): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing]
  def checkQueryKindZ(inp: Map[String, List[String]] = Map()): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing]
  def checkQueryKindXVariantC(inp: Map[String, List[String]] = Map()): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing]
  def checkQueryKindXVariantD(inp: Map[String, List[String]] = Map()): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing]
  def checkQueryVariantA(inp: Map[String, List[String]] = Map()): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing]
  def checkQueryVariantB(inp: Map[String, List[String]] = Map()): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing]

  final def transform: Transformation.PartiallyApplied[CheckQueryServiceGen[F]] = Transformation.of[CheckQueryServiceGen[F]](this)
}

object CheckQueryServiceGen extends Service.Mixin[CheckQueryServiceGen, CheckQueryServiceOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "CheckQueryService")
  val version: String = ""

  val hints: Hints = Hints(
    smithy.api.Mixin(localTraits = None),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[CheckQueryServiceOperation, _, _, _, _, _]] = Vector(
    CheckQueryServiceOperation.CheckQueryKindZVariantA,
    CheckQueryServiceOperation.CheckQueryKindYVariant,
    CheckQueryServiceOperation.CheckQueryKindZ,
    CheckQueryServiceOperation.CheckQueryKindXVariantC,
    CheckQueryServiceOperation.CheckQueryKindXVariantD,
    CheckQueryServiceOperation.CheckQueryVariantA,
    CheckQueryServiceOperation.CheckQueryVariantB,
  )

  def input[I, E, O, SI, SO](op: CheckQueryServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: CheckQueryServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: CheckQueryServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends CheckQueryServiceOperation.Transformed[CheckQueryServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: CheckQueryServiceGen[CheckQueryServiceOperation] = CheckQueryServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: CheckQueryServiceGen[P], f: PolyFunction5[P, P1]): CheckQueryServiceGen[P1] = new CheckQueryServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[CheckQueryServiceOperation, P]): CheckQueryServiceGen[P] = new CheckQueryServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: CheckQueryServiceGen[P]): PolyFunction5[CheckQueryServiceOperation, P] = CheckQueryServiceOperation.toPolyFunction(impl)

}

sealed trait CheckQueryServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: CheckQueryServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[CheckQueryServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object CheckQueryServiceOperation {

  object reified extends CheckQueryServiceGen[CheckQueryServiceOperation] {
    def checkQueryKindZVariantA(inp: Map[String, List[String]] = Map()): CheckQueryKindZVariantA = CheckQueryKindZVariantA(CheckQueryInput(inp))
    def checkQueryKindYVariant(inp: Map[String, List[String]] = Map()): CheckQueryKindYVariant = CheckQueryKindYVariant(CheckQueryInput(inp))
    def checkQueryKindZ(inp: Map[String, List[String]] = Map()): CheckQueryKindZ = CheckQueryKindZ(CheckQueryInput(inp))
    def checkQueryKindXVariantC(inp: Map[String, List[String]] = Map()): CheckQueryKindXVariantC = CheckQueryKindXVariantC(CheckQueryInput(inp))
    def checkQueryKindXVariantD(inp: Map[String, List[String]] = Map()): CheckQueryKindXVariantD = CheckQueryKindXVariantD(CheckQueryInput(inp))
    def checkQueryVariantA(inp: Map[String, List[String]] = Map()): CheckQueryVariantA = CheckQueryVariantA(CheckQueryInput(inp))
    def checkQueryVariantB(inp: Map[String, List[String]] = Map()): CheckQueryVariantB = CheckQueryVariantB(CheckQueryInput(inp))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: CheckQueryServiceGen[P], f: PolyFunction5[P, P1]) extends CheckQueryServiceGen[P1] {
    def checkQueryKindZVariantA(inp: Map[String, List[String]] = Map()): P1[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = f[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing](alg.checkQueryKindZVariantA(inp))
    def checkQueryKindYVariant(inp: Map[String, List[String]] = Map()): P1[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = f[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing](alg.checkQueryKindYVariant(inp))
    def checkQueryKindZ(inp: Map[String, List[String]] = Map()): P1[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = f[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing](alg.checkQueryKindZ(inp))
    def checkQueryKindXVariantC(inp: Map[String, List[String]] = Map()): P1[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = f[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing](alg.checkQueryKindXVariantC(inp))
    def checkQueryKindXVariantD(inp: Map[String, List[String]] = Map()): P1[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = f[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing](alg.checkQueryKindXVariantD(inp))
    def checkQueryVariantA(inp: Map[String, List[String]] = Map()): P1[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = f[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing](alg.checkQueryVariantA(inp))
    def checkQueryVariantB(inp: Map[String, List[String]] = Map()): P1[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = f[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing](alg.checkQueryVariantB(inp))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: CheckQueryServiceGen[P]): PolyFunction5[CheckQueryServiceOperation, P] = new PolyFunction5[CheckQueryServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: CheckQueryServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class CheckQueryKindZVariantA(input: CheckQueryInput) extends CheckQueryServiceOperation[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: CheckQueryServiceGen[F]): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = impl.checkQueryKindZVariantA(input.inp)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = CheckQueryKindZVariantA
  }
  object CheckQueryKindZVariantA extends smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    val schema: OperationSchema[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "CheckQueryKindZVariantA"))
      .withInput(CheckQueryInput.schema)
      .withOutput(CheckQueryOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/query-check?kind=z&variant=a"), code = 200), smithy.api.Readonly())
    def wrap(input: CheckQueryInput): CheckQueryKindZVariantA = CheckQueryKindZVariantA(input)
  }
  final case class CheckQueryKindYVariant(input: CheckQueryInput) extends CheckQueryServiceOperation[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: CheckQueryServiceGen[F]): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = impl.checkQueryKindYVariant(input.inp)
    def ordinal: Int = 1
    def endpoint: smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = CheckQueryKindYVariant
  }
  object CheckQueryKindYVariant extends smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    val schema: OperationSchema[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "CheckQueryKindYVariant"))
      .withInput(CheckQueryInput.schema)
      .withOutput(CheckQueryOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/query-check?kind=y&variant"), code = 200), smithy.api.Readonly())
    def wrap(input: CheckQueryInput): CheckQueryKindYVariant = CheckQueryKindYVariant(input)
  }
  final case class CheckQueryKindZ(input: CheckQueryInput) extends CheckQueryServiceOperation[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: CheckQueryServiceGen[F]): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = impl.checkQueryKindZ(input.inp)
    def ordinal: Int = 2
    def endpoint: smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = CheckQueryKindZ
  }
  object CheckQueryKindZ extends smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    val schema: OperationSchema[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "CheckQueryKindZ"))
      .withInput(CheckQueryInput.schema)
      .withOutput(CheckQueryOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/query-check?kind=z"), code = 200), smithy.api.Readonly())
    def wrap(input: CheckQueryInput): CheckQueryKindZ = CheckQueryKindZ(input)
  }
  final case class CheckQueryKindXVariantC(input: CheckQueryInput) extends CheckQueryServiceOperation[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: CheckQueryServiceGen[F]): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = impl.checkQueryKindXVariantC(input.inp)
    def ordinal: Int = 3
    def endpoint: smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = CheckQueryKindXVariantC
  }
  object CheckQueryKindXVariantC extends smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    val schema: OperationSchema[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "CheckQueryKindXVariantC"))
      .withInput(CheckQueryInput.schema)
      .withOutput(CheckQueryOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/query-check?kind=x&variant=c"), code = 200), smithy.api.Readonly())
    def wrap(input: CheckQueryInput): CheckQueryKindXVariantC = CheckQueryKindXVariantC(input)
  }
  final case class CheckQueryKindXVariantD(input: CheckQueryInput) extends CheckQueryServiceOperation[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: CheckQueryServiceGen[F]): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = impl.checkQueryKindXVariantD(input.inp)
    def ordinal: Int = 4
    def endpoint: smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = CheckQueryKindXVariantD
  }
  object CheckQueryKindXVariantD extends smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    val schema: OperationSchema[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "CheckQueryKindXVariantD"))
      .withInput(CheckQueryInput.schema)
      .withOutput(CheckQueryOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/query-check?kind=x&variant=d"), code = 200), smithy.api.Readonly())
    def wrap(input: CheckQueryInput): CheckQueryKindXVariantD = CheckQueryKindXVariantD(input)
  }
  final case class CheckQueryVariantA(input: CheckQueryInput) extends CheckQueryServiceOperation[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: CheckQueryServiceGen[F]): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = impl.checkQueryVariantA(input.inp)
    def ordinal: Int = 5
    def endpoint: smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = CheckQueryVariantA
  }
  object CheckQueryVariantA extends smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    val schema: OperationSchema[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "CheckQueryVariantA"))
      .withInput(CheckQueryInput.schema)
      .withOutput(CheckQueryOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/query-check?variant=a"), code = 200), smithy.api.Readonly())
    def wrap(input: CheckQueryInput): CheckQueryVariantA = CheckQueryVariantA(input)
  }
  final case class CheckQueryVariantB(input: CheckQueryInput) extends CheckQueryServiceOperation[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: CheckQueryServiceGen[F]): F[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = impl.checkQueryVariantB(input.inp)
    def ordinal: Int = 6
    def endpoint: smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = CheckQueryVariantB
  }
  object CheckQueryVariantB extends smithy4s.Endpoint[CheckQueryServiceOperation,CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] {
    val schema: OperationSchema[CheckQueryInput, Nothing, CheckQueryOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "CheckQueryVariantB"))
      .withInput(CheckQueryInput.schema)
      .withOutput(CheckQueryOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/query-check?variant=b"), code = 200), smithy.api.Readonly())
    def wrap(input: CheckQueryInput): CheckQueryVariantB = CheckQueryVariantB(input)
  }
}

