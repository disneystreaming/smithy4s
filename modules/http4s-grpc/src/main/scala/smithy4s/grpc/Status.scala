package smithy4s
package grpc

import alloy.proto.StatusDetails

case class Status(code: StatusCode, message: Option[String], details: StatusDetails)

object Status {

  val ok: Status = Status(StatusCode.Ok, Option.empty, StatusDetails(List.empty))

  // trait Foo[A] {
  //   def status(a: A, default: Status[A]): Status[A]
  // }

  // type ShapeIdExtractor[A] = A => Option[ShapeId]

  // object Decoder extends CachedSchemaCompiler.Impl[Foo] {
  //   type Aux[A] = ShapeIdExtractor[A]

  //   override def fromSchema[A](schema: Schema[A], cache: Cache): Foo[A] =
  //     (value, default) => {
  //       val schemaCompiler = schema.compile(new GrpcStatusSchemaVisitor(cache))
  //       schemaCompiler(value).getOrElse(default)
  //     }
  // }

  // private[grpc] class GrpcStatusSchemaVisitor(val cache: CompilationCache[ShapeIdExtractor]) extends  SchemaVisitor.Cached[ShapeIdExtractor]
  //   with SchemaVisitor.Default[ShapeIdExtractor] { compile =>

  //     override def default[A]: A => Option[StatusCode] = _ => None

  //     override def union[U](
  //         shapeId: ShapeId,
  //         hints: Hints,
  //         alternatives: Vector[Alt[U, _]],
  //         dispatcher: Alt.Dispatcher[U]
  //     ): ShapeIdExtractor[U] = {
  //       dispatcher.compile(new Alt.Precompiler[ShapeIdExtractor] {
  //         def apply[A](label: String, instance: Schema[A]): ShapeIdExtractor[A] =
  //           compile(instance)
  //       })
  //     }

  //     override def lazily[A](suspend: Lazy[Schema[A]]): ShapeIdExtractor[A] =
  //       apply(suspend.value)

  //     override def biject[A, B](
  //         schema: Schema[A],
  //         bijection: Bijection[A, B]
  //     ): ShapeIdExtractor[B] = {
  //       val codeExtractor = apply(schema)
  //       b => codeExtractor(bijection.from(b))
  //     }

  //     override def refine[A, B](
  //         schema: Schema[A],
  //         refinement: Refinement[A, B]
  //     ): ShapeIdExtractor[B] = {
  //       val codeExtractor = apply(schema)
  //       b => codeExtractor(refinement.from(b))
  //     }

  //     override def struct[S](
  //         shapeId: ShapeId,
  //         hints: Hints,
  //         fields: Vector[Field[S, _]],
  //         make: IndexedSeq[Any] => S
  //     ): ShapeIdExtractor[S] = default => {
  //       //FIXME: We probably need some kind of @grpcErrorCode annotation
  //       hints.get(alloy.proto.GrpcError).map{ grpcErrorTrait =>
  //         ???
  //       }
  //       // hints.get(smithy.api.Error).map{
  //       //   case smithy.api.Error.CLIENT => StatusCode.InvalidArgument
  //       //   case smithy.api.Error.SERVER => StatusCode.Internal
  //       // }
  //     }
  //   }
}
