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
import smithy4s.schema.Schema.unit

trait LibraryGen[F[_, _, _, _, _]] {
  self =>

  def listPublishers(): F[Unit, Nothing, ListPublishersOutput, Nothing, Nothing]
  def getBook(): F[Unit, Nothing, Unit, Nothing, Nothing]
  def buyBook(): F[Unit, Nothing, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[LibraryGen[F]] = Transformation.of[LibraryGen[F]](this)
}

object LibraryGen extends Service.Mixin[LibraryGen, LibraryOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "Library")
  val version: String = ""

  val hints: Hints = Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[LibraryOperation, _, _, _, _, _]] = Vector(
    LibraryOperation.ListPublishers,
    LibraryOperation.GetBook,
    LibraryOperation.BuyBook,
  )

  def input[I, E, O, SI, SO](op: LibraryOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: LibraryOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: LibraryOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends LibraryOperation.Transformed[LibraryOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: LibraryGen[LibraryOperation] = LibraryOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: LibraryGen[P], f: PolyFunction5[P, P1]): LibraryGen[P1] = new LibraryOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[LibraryOperation, P]): LibraryGen[P] = new LibraryOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: LibraryGen[P]): PolyFunction5[LibraryOperation, P] = LibraryOperation.toPolyFunction(impl)

}

sealed trait LibraryOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: LibraryGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[LibraryOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object LibraryOperation {

  object reified extends LibraryGen[LibraryOperation] {
    def listPublishers() = ListPublishers()
    def getBook() = GetBook()
    def buyBook() = BuyBook()
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: LibraryGen[P], f: PolyFunction5[P, P1]) extends LibraryGen[P1] {
    def listPublishers() = f[Unit, Nothing, ListPublishersOutput, Nothing, Nothing](alg.listPublishers())
    def getBook() = f[Unit, Nothing, Unit, Nothing, Nothing](alg.getBook())
    def buyBook() = f[Unit, Nothing, Unit, Nothing, Nothing](alg.buyBook())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: LibraryGen[P]): PolyFunction5[LibraryOperation, P] = new PolyFunction5[LibraryOperation, P] {
    def apply[I, E, O, SI, SO](op: LibraryOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class ListPublishers() extends LibraryOperation[Unit, Nothing, ListPublishersOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: LibraryGen[F]): F[Unit, Nothing, ListPublishersOutput, Nothing, Nothing] = impl.listPublishers()
    def ordinal = 0
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[LibraryOperation,Unit, Nothing, ListPublishersOutput, Nothing, Nothing] = ListPublishers
  }
  object ListPublishers extends smithy4s.Endpoint[LibraryOperation,Unit, Nothing, ListPublishersOutput, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, ListPublishersOutput, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "ListPublishers"))
      .withInput(unit.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withOutput(ListPublishersOutput.schema.addHints(smithy4s.internals.InputOutput.Output.widen))
      .withHints(smithy.api.Readonly())
    def wrap(input: Unit) = ListPublishers()
  }
  final case class GetBook() extends LibraryOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: LibraryGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl.getBook()
    def ordinal = 1
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[LibraryOperation,Unit, Nothing, Unit, Nothing, Nothing] = GetBook
  }
  object GetBook extends smithy4s.Endpoint[LibraryOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "GetBook"))
      .withInput(unit.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
      .withHints(smithy.api.Readonly())
    def wrap(input: Unit) = GetBook()
  }
  final case class BuyBook() extends LibraryOperation[Unit, Nothing, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: LibraryGen[F]): F[Unit, Nothing, Unit, Nothing, Nothing] = impl.buyBook()
    def ordinal = 2
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[LibraryOperation,Unit, Nothing, Unit, Nothing, Nothing] = BuyBook
  }
  object BuyBook extends smithy4s.Endpoint[LibraryOperation,Unit, Nothing, Unit, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "BuyBook"))
      .withInput(unit.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: Unit) = BuyBook()
  }
}

