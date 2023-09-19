package smithy4s.example

import smithy4s.Endpoint
import smithy4s.Errorable
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.OperationSchema
import smithy4s.schema.Schema.UnionSchema
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union
import smithy4s.schema.Schema.unit

trait KVStoreGen[F[_, _, _, _, _]] {
  self =>

  def get(key: String): F[Key, KVStoreOperation.GetError, Value, Nothing, Nothing]
  def put(key: String, value: String): F[KeyValue, KVStoreOperation.PutError, Unit, Nothing, Nothing]
  def delete(key: String): F[Key, KVStoreOperation.DeleteError, Unit, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[KVStoreGen[F]] = Transformation.of[KVStoreGen[F]](this)
}

object KVStoreGen extends Service.Mixin[KVStoreGen, KVStoreOperation] {

  val id: ShapeId = ShapeId("smithy4s.example", "KVStore")
  val version: String = ""

  val hints: Hints = Hints.empty

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[KVStoreOperation, _, _, _, _, _]] = Vector(
    KVStoreOperation.Get,
    KVStoreOperation.Put,
    KVStoreOperation.Delete,
  )

  def input[I, E, O, SI, SO](op: KVStoreOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: KVStoreOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: KVStoreOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends KVStoreOperation.Transformed[KVStoreOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: KVStoreGen[KVStoreOperation] = KVStoreOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: KVStoreGen[P], f: PolyFunction5[P, P1]): KVStoreGen[P1] = new KVStoreOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[KVStoreOperation, P]): KVStoreGen[P] = new KVStoreOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: KVStoreGen[P]): PolyFunction5[KVStoreOperation, P] = KVStoreOperation.toPolyFunction(impl)

  type GetError = KVStoreOperation.GetError
  val GetError = KVStoreOperation.GetError
  type PutError = KVStoreOperation.PutError
  val PutError = KVStoreOperation.PutError
  type DeleteError = KVStoreOperation.DeleteError
  val DeleteError = KVStoreOperation.DeleteError
}

sealed trait KVStoreOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: KVStoreGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[KVStoreOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object KVStoreOperation {

  object reified extends KVStoreGen[KVStoreOperation] {
    def get(key: String) = Get(Key(key))
    def put(key: String, value: String) = Put(KeyValue(key, value))
    def delete(key: String) = Delete(Key(key))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: KVStoreGen[P], f: PolyFunction5[P, P1]) extends KVStoreGen[P1] {
    def get(key: String) = f[Key, KVStoreOperation.GetError, Value, Nothing, Nothing](alg.get(key))
    def put(key: String, value: String) = f[KeyValue, KVStoreOperation.PutError, Unit, Nothing, Nothing](alg.put(key, value))
    def delete(key: String) = f[Key, KVStoreOperation.DeleteError, Unit, Nothing, Nothing](alg.delete(key))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: KVStoreGen[P]): PolyFunction5[KVStoreOperation, P] = new PolyFunction5[KVStoreOperation, P] {
    def apply[I, E, O, SI, SO](op: KVStoreOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class Get(input: Key) extends KVStoreOperation[Key, KVStoreOperation.GetError, Value, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: KVStoreGen[F]): F[Key, KVStoreOperation.GetError, Value, Nothing, Nothing] = impl.get(input.key)
    def ordinal = 0
    def endpoint: smithy4s.Endpoint[KVStoreOperation,Key, KVStoreOperation.GetError, Value, Nothing, Nothing] = Get
  }
  object Get extends smithy4s.Endpoint[KVStoreOperation,Key, KVStoreOperation.GetError, Value, Nothing, Nothing] {
    def schema: OperationSchema[Key, KVStoreOperation.GetError, Value, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Get"))
      .withInput(Key.schema.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withError(GetError)
      .withOutput(Value.schema.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: Key) = Get(input)
  }
  sealed trait GetError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: GetError = this
    def $ordinal: Int

    object project {
      def unauthorizedError: Option[UnauthorizedError] = GetError.UnauthorizedErrorCase.alt.project.lift(self).map(_.unauthorizedError)
      def keyNotFoundError: Option[KeyNotFoundError] = GetError.KeyNotFoundErrorCase.alt.project.lift(self).map(_.keyNotFoundError)
    }

    def accept[A](visitor: GetError.Visitor[A]): A = this match {
      case value: GetError.UnauthorizedErrorCase => visitor.unauthorizedError(value.unauthorizedError)
      case value: GetError.KeyNotFoundErrorCase => visitor.keyNotFoundError(value.keyNotFoundError)
    }
  }
  object GetError extends Errorable.Companion[GetError] {

    def unauthorizedError(unauthorizedError: UnauthorizedError): GetError = UnauthorizedErrorCase(unauthorizedError)
    def keyNotFoundError(keyNotFoundError: KeyNotFoundError): GetError = KeyNotFoundErrorCase(keyNotFoundError)

    val id: ShapeId = ShapeId("smithy4s.example", "GetError")

    val hints: Hints = Hints.empty

    final case class UnauthorizedErrorCase(unauthorizedError: UnauthorizedError) extends GetError { final def $ordinal: Int = 0 }
    final case class KeyNotFoundErrorCase(keyNotFoundError: KeyNotFoundError) extends GetError { final def $ordinal: Int = 1 }

    object UnauthorizedErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GetError.UnauthorizedErrorCase] = bijection(UnauthorizedError.schema.addHints(hints), GetError.UnauthorizedErrorCase(_), _.unauthorizedError)
      val alt = schema.oneOf[GetError]("UnauthorizedError")
    }
    object KeyNotFoundErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GetError.KeyNotFoundErrorCase] = bijection(KeyNotFoundError.schema.addHints(hints), GetError.KeyNotFoundErrorCase(_), _.keyNotFoundError)
      val alt = schema.oneOf[GetError]("KeyNotFoundError")
    }

    trait Visitor[A] {
      def unauthorizedError(value: UnauthorizedError): A
      def keyNotFoundError(value: KeyNotFoundError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def unauthorizedError(value: UnauthorizedError): A = default
        def keyNotFoundError(value: KeyNotFoundError): A = default
      }
    }

    implicit val schema: UnionSchema[GetError] = union(
      GetError.UnauthorizedErrorCase.alt,
      GetError.KeyNotFoundErrorCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[GetError] = throwable match {
      case e: UnauthorizedError => Some(GetError.UnauthorizedErrorCase(e))
      case e: KeyNotFoundError => Some(GetError.KeyNotFoundErrorCase(e))
      case _ => None
    }
    def unliftError(e: GetError): Throwable = e match {
      case GetError.UnauthorizedErrorCase(e) => e
      case GetError.KeyNotFoundErrorCase(e) => e
    }
  }
  final case class Put(input: KeyValue) extends KVStoreOperation[KeyValue, KVStoreOperation.PutError, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: KVStoreGen[F]): F[KeyValue, KVStoreOperation.PutError, Unit, Nothing, Nothing] = impl.put(input.key, input.value)
    def ordinal = 1
    def endpoint: smithy4s.Endpoint[KVStoreOperation,KeyValue, KVStoreOperation.PutError, Unit, Nothing, Nothing] = Put
  }
  object Put extends smithy4s.Endpoint[KVStoreOperation,KeyValue, KVStoreOperation.PutError, Unit, Nothing, Nothing] {
    def schema: OperationSchema[KeyValue, KVStoreOperation.PutError, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Put"))
      .withInput(KeyValue.schema.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withError(PutError)
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: KeyValue) = Put(input)
  }
  sealed trait PutError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: PutError = this
    def $ordinal: Int

    object project {
      def unauthorizedError: Option[UnauthorizedError] = PutError.UnauthorizedErrorCase.alt.project.lift(self).map(_.unauthorizedError)
    }

    def accept[A](visitor: PutError.Visitor[A]): A = this match {
      case value: PutError.UnauthorizedErrorCase => visitor.unauthorizedError(value.unauthorizedError)
    }
  }
  object PutError extends Errorable.Companion[PutError] {

    def unauthorizedError(unauthorizedError: UnauthorizedError): PutError = UnauthorizedErrorCase(unauthorizedError)

    val id: ShapeId = ShapeId("smithy4s.example", "PutError")

    val hints: Hints = Hints.empty

    final case class UnauthorizedErrorCase(unauthorizedError: UnauthorizedError) extends PutError { final def $ordinal: Int = 0 }

    object UnauthorizedErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[PutError.UnauthorizedErrorCase] = bijection(UnauthorizedError.schema.addHints(hints), PutError.UnauthorizedErrorCase(_), _.unauthorizedError)
      val alt = schema.oneOf[PutError]("UnauthorizedError")
    }

    trait Visitor[A] {
      def unauthorizedError(value: UnauthorizedError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def unauthorizedError(value: UnauthorizedError): A = default
      }
    }

    implicit val schema: UnionSchema[PutError] = union(
      PutError.UnauthorizedErrorCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[PutError] = throwable match {
      case e: UnauthorizedError => Some(PutError.UnauthorizedErrorCase(e))
      case _ => None
    }
    def unliftError(e: PutError): Throwable = e match {
      case PutError.UnauthorizedErrorCase(e) => e
    }
  }
  final case class Delete(input: Key) extends KVStoreOperation[Key, KVStoreOperation.DeleteError, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: KVStoreGen[F]): F[Key, KVStoreOperation.DeleteError, Unit, Nothing, Nothing] = impl.delete(input.key)
    def ordinal = 2
    def endpoint: smithy4s.Endpoint[KVStoreOperation,Key, KVStoreOperation.DeleteError, Unit, Nothing, Nothing] = Delete
  }
  object Delete extends smithy4s.Endpoint[KVStoreOperation,Key, KVStoreOperation.DeleteError, Unit, Nothing, Nothing] {
    def schema: OperationSchema[Key, KVStoreOperation.DeleteError, Unit, Nothing, Nothing] = Schema.operation(ShapeId("smithy4s.example", "Delete"))
      .withInput(Key.schema.addHints(smithy4s.internals.InputOutput.Input.widen))
      .withError(DeleteError)
      .withOutput(unit.addHints(smithy4s.internals.InputOutput.Output.widen))
    def wrap(input: Key) = Delete(input)
  }
  sealed trait DeleteError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: DeleteError = this
    def $ordinal: Int

    object project {
      def unauthorizedError: Option[UnauthorizedError] = DeleteError.UnauthorizedErrorCase.alt.project.lift(self).map(_.unauthorizedError)
      def keyNotFoundError: Option[KeyNotFoundError] = DeleteError.KeyNotFoundErrorCase.alt.project.lift(self).map(_.keyNotFoundError)
    }

    def accept[A](visitor: DeleteError.Visitor[A]): A = this match {
      case value: DeleteError.UnauthorizedErrorCase => visitor.unauthorizedError(value.unauthorizedError)
      case value: DeleteError.KeyNotFoundErrorCase => visitor.keyNotFoundError(value.keyNotFoundError)
    }
  }
  object DeleteError extends Errorable.Companion[DeleteError] {

    def unauthorizedError(unauthorizedError: UnauthorizedError): DeleteError = UnauthorizedErrorCase(unauthorizedError)
    def keyNotFoundError(keyNotFoundError: KeyNotFoundError): DeleteError = KeyNotFoundErrorCase(keyNotFoundError)

    val id: ShapeId = ShapeId("smithy4s.example", "DeleteError")

    val hints: Hints = Hints.empty

    final case class UnauthorizedErrorCase(unauthorizedError: UnauthorizedError) extends DeleteError { final def $ordinal: Int = 0 }
    final case class KeyNotFoundErrorCase(keyNotFoundError: KeyNotFoundError) extends DeleteError { final def $ordinal: Int = 1 }

    object UnauthorizedErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[DeleteError.UnauthorizedErrorCase] = bijection(UnauthorizedError.schema.addHints(hints), DeleteError.UnauthorizedErrorCase(_), _.unauthorizedError)
      val alt = schema.oneOf[DeleteError]("UnauthorizedError")
    }
    object KeyNotFoundErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[DeleteError.KeyNotFoundErrorCase] = bijection(KeyNotFoundError.schema.addHints(hints), DeleteError.KeyNotFoundErrorCase(_), _.keyNotFoundError)
      val alt = schema.oneOf[DeleteError]("KeyNotFoundError")
    }

    trait Visitor[A] {
      def unauthorizedError(value: UnauthorizedError): A
      def keyNotFoundError(value: KeyNotFoundError): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def unauthorizedError(value: UnauthorizedError): A = default
        def keyNotFoundError(value: KeyNotFoundError): A = default
      }
    }

    implicit val schema: UnionSchema[DeleteError] = union(
      DeleteError.UnauthorizedErrorCase.alt,
      DeleteError.KeyNotFoundErrorCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[DeleteError] = throwable match {
      case e: UnauthorizedError => Some(DeleteError.UnauthorizedErrorCase(e))
      case e: KeyNotFoundError => Some(DeleteError.KeyNotFoundErrorCase(e))
      case _ => None
    }
    def unliftError(e: DeleteError): Throwable = e match {
      case DeleteError.UnauthorizedErrorCase(e) => e
      case DeleteError.KeyNotFoundErrorCase(e) => e
    }
  }
}

