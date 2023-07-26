package smithy4s.example.aws

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5

trait MyThingGen[F[_, _, _, _, _]] {
  self =>


  def transform: Transformation.PartiallyApplied[MyThingGen[F]] = Transformation.of[MyThingGen[F]](this)
}

object MyThingGen extends smithy4s.Service.Mixin[MyThingGen, MyThingOperation] {

  val id: ShapeId = ShapeId("smithy4s.example.aws", "MyAwsService")
  val version: String = ""

  val hints: Hints = Hints(
    aws.api.Service(sdkId = "MyThing", arnNamespace = None, cloudFormationName = None, cloudTrailEventSource = None, endpointPrefix = Some("mything")),
  )

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[MyThingOperation, _, _, _, _, _]] = Vector()

  def input[I, E, O, SI, SO](op: MyThingOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: MyThingOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: MyThingOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends MyThingOperation.Transformed[MyThingOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: MyThingGen[MyThingOperation] = MyThingOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: MyThingGen[P], f: PolyFunction5[P, P1]): MyThingGen[P1] = new MyThingOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[MyThingOperation, P]): MyThingGen[P] = new MyThingOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: MyThingGen[P]): PolyFunction5[MyThingOperation, P] = MyThingOperation.toPolyFunction(impl)

}

sealed trait MyThingOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: MyThingGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[MyThingOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object MyThingOperation {

  object reified extends MyThingGen[MyThingOperation] {
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: MyThingGen[P], f: PolyFunction5[P, P1]) extends MyThingGen[P1] {
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: MyThingGen[P]): PolyFunction5[MyThingOperation, P] = new PolyFunction5[MyThingOperation, P] {
    def apply[I, E, O, SI, SO](op: MyThingOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = sys.error("impossible")
  }
}

