package smithy4s

package object benchmark {
  type BenchmarkService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[BenchmarkServiceGen, F]
  val BenchmarkService = BenchmarkServiceGen

  type ListPermissions = smithy4s.benchmark.ListPermissions.Type
  type ListMetadata = smithy4s.benchmark.ListMetadata.Type
  type ListTags = smithy4s.benchmark.ListTags.Type

}