package smithy4s.example

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
  }
  object Get extends smithy4s.Endpoint[KVStoreOperation,Key, KVStoreOperation.GetError, Value, Nothing, Nothing] with Errorable[GetError] {
    val id: ShapeId = ShapeId("smithy4s.example", "Get")
    val input: Schema[Key] = Key.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Value] = Value.schema.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints.empty
    def wrap(input: Key) = Get(input)
    override val errorable: Option[Errorable[GetError]] = Some(this)
    val error: UnionSchema[GetError] = GetError.schema
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
  sealed trait GetError extends scala.Product with scala.Serializable {
    @inline final def widen: GetError = this
    def _ordinal: Int
  }
  object GetError extends ShapeTag.Companion[GetError] {
    val id: ShapeId = ShapeId("smithy4s.example", "GetError")

    val hints: Hints = Hints.empty

    final case class UnauthorizedErrorCase(unauthorizedError: UnauthorizedError) extends GetError { final def _ordinal: Int = 0 }
    final case class KeyNotFoundErrorCase(keyNotFoundError: KeyNotFoundError) extends GetError { final def _ordinal: Int = 1 }

    object UnauthorizedErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[UnauthorizedErrorCase] = bijection(UnauthorizedError.schema.addHints(hints), UnauthorizedErrorCase(_), _.unauthorizedError)
      val alt = schema.oneOf[GetError]("UnauthorizedError")
    }
    object KeyNotFoundErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[KeyNotFoundErrorCase] = bijection(KeyNotFoundError.schema.addHints(hints), KeyNotFoundErrorCase(_), _.keyNotFoundError)
      val alt = schema.oneOf[GetError]("KeyNotFoundError")
    }

    implicit val schema: UnionSchema[GetError] = union(
      UnauthorizedErrorCase.alt,
      KeyNotFoundErrorCase.alt,
    ){
      _._ordinal
    }
  }
  final case class Put(input: KeyValue) extends KVStoreOperation[KeyValue, KVStoreOperation.PutError, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: KVStoreGen[F]): F[KeyValue, KVStoreOperation.PutError, Unit, Nothing, Nothing] = impl.put(input.key, input.value)
    def ordinal = 1
  }
  object Put extends smithy4s.Endpoint[KVStoreOperation,KeyValue, KVStoreOperation.PutError, Unit, Nothing, Nothing] with Errorable[PutError] {
    val id: ShapeId = ShapeId("smithy4s.example", "Put")
    val input: Schema[KeyValue] = KeyValue.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints.empty
    def wrap(input: KeyValue) = Put(input)
    override val errorable: Option[Errorable[PutError]] = Some(this)
    val error: UnionSchema[PutError] = PutError.schema
    def liftError(throwable: Throwable): Option[PutError] = throwable match {
      case e: UnauthorizedError => Some(PutError.UnauthorizedErrorCase(e))
      case _ => None
    }
    def unliftError(e: PutError): Throwable = e match {
      case PutError.UnauthorizedErrorCase(e) => e
    }
  }
  sealed trait PutError extends scala.Product with scala.Serializable {
    @inline final def widen: PutError = this
    def _ordinal: Int
  }
  object PutError extends ShapeTag.Companion[PutError] {
    val id: ShapeId = ShapeId("smithy4s.example", "PutError")

    val hints: Hints = Hints.empty

    final case class UnauthorizedErrorCase(unauthorizedError: UnauthorizedError) extends PutError { final def _ordinal: Int = 0 }

    object UnauthorizedErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[UnauthorizedErrorCase] = bijection(UnauthorizedError.schema.addHints(hints), UnauthorizedErrorCase(_), _.unauthorizedError)
      val alt = schema.oneOf[PutError]("UnauthorizedError")
    }

    implicit val schema: UnionSchema[PutError] = union(
      UnauthorizedErrorCase.alt,
    ){
      _._ordinal
    }
  }
  final case class Delete(input: Key) extends KVStoreOperation[Key, KVStoreOperation.DeleteError, Unit, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: KVStoreGen[F]): F[Key, KVStoreOperation.DeleteError, Unit, Nothing, Nothing] = impl.delete(input.key)
    def ordinal = 2
  }
  object Delete extends smithy4s.Endpoint[KVStoreOperation,Key, KVStoreOperation.DeleteError, Unit, Nothing, Nothing] with Errorable[DeleteError] {
    val id: ShapeId = ShapeId("smithy4s.example", "Delete")
    val input: Schema[Key] = Key.schema.addHints(smithy4s.internals.InputOutput.Input.widen)
    val output: Schema[Unit] = unit.addHints(smithy4s.internals.InputOutput.Output.widen)
    val streamedInput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val streamedOutput: StreamingSchema[Nothing] = StreamingSchema.nothing
    val hints: Hints = Hints.empty
    def wrap(input: Key) = Delete(input)
    override val errorable: Option[Errorable[DeleteError]] = Some(this)
    val error: UnionSchema[DeleteError] = DeleteError.schema
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
  sealed trait DeleteError extends scala.Product with scala.Serializable {
    @inline final def widen: DeleteError = this
    def _ordinal: Int
  }
  object DeleteError extends ShapeTag.Companion[DeleteError] {
    val id: ShapeId = ShapeId("smithy4s.example", "DeleteError")

    val hints: Hints = Hints.empty

    final case class UnauthorizedErrorCase(unauthorizedError: UnauthorizedError) extends DeleteError { final def _ordinal: Int = 0 }
    final case class KeyNotFoundErrorCase(keyNotFoundError: KeyNotFoundError) extends DeleteError { final def _ordinal: Int = 1 }

    object UnauthorizedErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[UnauthorizedErrorCase] = bijection(UnauthorizedError.schema.addHints(hints), UnauthorizedErrorCase(_), _.unauthorizedError)
      val alt = schema.oneOf[DeleteError]("UnauthorizedError")
    }
    object KeyNotFoundErrorCase {
      val hints: Hints = Hints.empty
      val schema: Schema[KeyNotFoundErrorCase] = bijection(KeyNotFoundError.schema.addHints(hints), KeyNotFoundErrorCase(_), _.keyNotFoundError)
      val alt = schema.oneOf[DeleteError]("KeyNotFoundError")
    }

    implicit val schema: UnionSchema[DeleteError] = union(
      UnauthorizedErrorCase.alt,
      KeyNotFoundErrorCase.alt,
    ){
      _._ordinal
    }
  }
}

