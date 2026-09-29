package smithy4s.example

package object aws {
  type DottedPrefix[F[_]] = smithy4s.kinds.FunctorAlgebra[DottedPrefixGen, F]
  val DottedPrefix = DottedPrefixGen
  type MyThing[F[_]] = smithy4s.kinds.FunctorAlgebra[MyThingGen, F]
  val MyThing = MyThingGen
  type PrefixDiffersFromSigningName[F[_]] = smithy4s.kinds.FunctorAlgebra[PrefixDiffersFromSigningNameGen, F]
  val PrefixDiffersFromSigningName = PrefixDiffersFromSigningNameGen
  type RestJsonSes[F[_]] = smithy4s.kinds.FunctorAlgebra[RestJsonSesGen, F]
  val RestJsonSes = RestJsonSesGen
  type NoEndpointPrefix[F[_]] = smithy4s.kinds.FunctorAlgebra[NoEndpointPrefixGen, F]
  val NoEndpointPrefix = NoEndpointPrefixGen
  type NoSigv4[F[_]] = smithy4s.kinds.FunctorAlgebra[NoSigv4Gen, F]
  val NoSigv4 = NoSigv4Gen


}