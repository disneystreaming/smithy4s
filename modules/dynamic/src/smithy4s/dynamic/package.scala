package smithy4s

package object dynamic {

  type DynData = Any
  type DynStruct = Array[DynData]
  type DynAlt = (Int, DynData)

  type DynamicAlg[F[_, _, _, _, _]] = Transformation[DynamicOp, F]

}
