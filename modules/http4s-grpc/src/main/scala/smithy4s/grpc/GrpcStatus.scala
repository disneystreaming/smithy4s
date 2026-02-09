package smithy4s
package grpc

import smithy4s.schema._

sealed abstract class GrpcStatus(val code: Int) extends  Product with Serializable

object GrpcStatus {

  sealed abstract class Failed(code: Int) extends GrpcStatus(code)
  case object Ok extends GrpcStatus(code = 0)
  case class Unknown(override val code: Int) extends GrpcStatus(code)
  case object InvalidArgument extends GrpcStatus(code = 3)
  case object DeadlineExceeded extends GrpcStatus(code = 4)
  case object NotFound extends GrpcStatus(code = 5)
  case object AlreadyExists extends GrpcStatus(code = 6)
  case object PermissionDenied extends GrpcStatus(code = 7)
  case object ResourceExhausted extends GrpcStatus(code = 8)
  case object FailedPrecondition extends GrpcStatus(code = 9)
  case object Aborted extends GrpcStatus(code = 10)
  case object OutOfRange extends GrpcStatus(code = 11)
  case object Unimplemented extends GrpcStatus(code = 12)
  case object Internal extends GrpcStatus(code = 13)
  case object Unavailable extends GrpcStatus(code = 14)
  case object DataLoss extends GrpcStatus(code = 15)  
  case object Unauthenticated extends GrpcStatus(code = 16)

  def fromStatusCode(statusCode: Int): Option[GrpcStatus] = statusValues.find(_.code == statusCode)

  val statusValues: List[GrpcStatus] = List(
    Ok,
  )

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
          case smithy.api.Error.CLIENT => GrpcStatus.Unknown(400)
          case smithy.api.Error.SERVER => GrpcStatus.Unknown(500)
        }
      }
    }
}
