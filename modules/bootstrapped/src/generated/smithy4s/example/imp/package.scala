package smithy4s.example

package object imp {
  type ImportService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[ImportServiceGen, F]
  val ImportService = ImportServiceGen


}