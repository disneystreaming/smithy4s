package smithy4s.example


trait EmptyServiceGen[F[_, _, _, _, _]] {
  self =>


  def transform[G[_, _, _, _, _]](transformation : smithy4s.Transformation[F, G]) : EmptyServiceGen[G] = new Transformed(transformation)
  class Transformed[G[_, _, _, _, _]](transformation : smithy4s.Transformation[F, G]) extends EmptyServiceGen[G] {
  }
}

object EmptyServiceGen extends smithy4s.Service[EmptyServiceGen, EmptyServiceOperation] {

  def apply[F[_]](implicit F: smithy4s.Monadic[EmptyServiceGen, F]): F.type = F

  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "EmptyService")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  val endpoints = List()

  val version: String = "1.0"

  def endpoint[I, E, O, SI, SO](op : EmptyServiceOperation[I, E, O, SI, SO]) = sys.error("impossible")

  object reified extends EmptyServiceGen[EmptyServiceOperation] {
  }

  def transform[P[_, _, _, _, _]](transformation: smithy4s.Transformation[EmptyServiceOperation, P]): EmptyServiceGen[P] = reified.transform(transformation)

  def transform[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: EmptyServiceGen[P], transformation: smithy4s.Transformation[P, P1]): EmptyServiceGen[P1] = alg.transform(transformation)

  def asTransformation[P[_, _, _, _, _]](impl : EmptyServiceGen[P]): smithy4s.Transformation[EmptyServiceOperation, P] = new smithy4s.Transformation[EmptyServiceOperation, P] {
    def apply[I, E, O, SI, SO](op : EmptyServiceOperation[I, E, O, SI, SO]) : P[I, E, O, SI, SO] = sys.error("impossible")
  }
}

sealed trait EmptyServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput]
