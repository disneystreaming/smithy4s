package smithy4s
package grpc

import smithy4s.schema._

sealed abstract class GrpcStatus(val code: Int) extends  Product with Serializable

object GrpcStatus {

  sealed abstract class Failed(code: Int) extends GrpcStatus(code)
  case object Ok extends GrpcStatus(code = 0)
  case object Cancelled extends Failed(code = 1)
  case object Unknown extends Failed(code = 2)
  case object InvalidArgument extends Failed(code = 3)
  case object DeadlineExceeded extends Failed(code = 4)
  case object NotFound extends Failed(code = 5)
  case object AlreadyExists extends Failed(code = 6)
  case object PermissionDenied extends Failed(code = 7)
  case object ResourceExhausted extends Failed(code = 8)
  case object FailedPrecondition extends Failed(code = 9)
  case object Aborted extends Failed(code = 10)
  case object OutOfRange extends Failed(code = 11)
  case object Unimplemented extends Failed(code = 12)
  case object Internal extends Failed(code = 13)
  case object Unavailable extends Failed(code = 14)
  case object DataLoss extends Failed(code = 15)  
  case object Unauthenticated extends Failed(code = 16)

  private lazy val statusByCode: Map[Int, GrpcStatus] = List(
    Ok,
    Cancelled,
    Unknown,
    InvalidArgument,
    DeadlineExceeded,
    NotFound,
    AlreadyExists,
    PermissionDenied,
    ResourceExhausted,
    FailedPrecondition,
    Aborted,
    OutOfRange,
    Unimplemented,
    Internal,
    Unavailable,
    DataLoss,
    Unauthenticated
  ).map(s => (s.code, s))
  .toMap

  def fromStatusCode(statusCode: Int): GrpcStatus = 
    statusByCode
      .get(statusCode)
      // https://github.com/grpc/grpc/blob/master/doc/statuscodes.md
      // All RPCs started at a client return a status object composed of an integer
      // code and a string message.
      // The server-side can choose the status it returns for a given RPC.
      // Applications should only use values defined above.
      // gRPC libraries that encounter values outside this range must either propagate 
      // them directly or convert them to UNKNOWN.
      .getOrElse(Unknown) // we chose to default to UNKNOWN


  trait Status[A] {
    def status(a: A, default: GrpcStatus): GrpcStatus
  }

  type CodeExtractor[A] = A => Option[GrpcStatus]

  object Decoder extends CachedSchemaCompiler.Impl[Status] {
    type Aux[A] = CodeExtractor[A]

    override def fromSchema[A](schema: Schema[A], cache: Cache): Status[A] =
      (value, default) => {
        val schemaCompiler = schema.compile(new GrpcStatusSchemaVisitor(cache))
        schemaCompiler(value).getOrElse(default)
      }
  }

  private[grpc] class GrpcStatusSchemaVisitor(val cache: CompilationCache[CodeExtractor]) extends  SchemaVisitor.Cached[CodeExtractor]
    with SchemaVisitor.Default[CodeExtractor] { compile =>

      override def default[A]: A => Option[GrpcStatus] = _ => None

      override def union[U](
          shapeId: ShapeId,
          hints: Hints,
          alternatives: Vector[Alt[U, _]],
          dispatcher: Alt.Dispatcher[U]
      ): CodeExtractor[U] = {
        dispatcher.compile(new Alt.Precompiler[CodeExtractor] {
          def apply[A](label: String, instance: Schema[A]): CodeExtractor[A] =
            compile(instance)
        })
      }

      override def lazily[A](suspend: Lazy[Schema[A]]): CodeExtractor[A] =
        apply(suspend.value)

      override def biject[A, B](
          schema: Schema[A],
          bijection: Bijection[A, B]
      ): CodeExtractor[B] = {
        val codeExtractor = apply(schema)
        b => codeExtractor(bijection.from(b))
      }

      override def refine[A, B](
          schema: Schema[A],
          refinement: Refinement[A, B]
      ): CodeExtractor[B] = {
        val codeExtractor = apply(schema)
        b => codeExtractor(refinement.from(b))
      }

      override def struct[S](
          shapeId: ShapeId,
          hints: Hints,
          fields: Vector[Field[S, _]],
          make: IndexedSeq[Any] => S
      ): CodeExtractor[S] = default => {
        //FIXME: We probably need some kind of @grpcErrorCode annotation
        hints.get(smithy.api.Error).map{
          case smithy.api.Error.CLIENT => GrpcStatus.InvalidArgument
          case smithy.api.Error.SERVER => GrpcStatus.Internal
        }
      }
    }
}
