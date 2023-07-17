package smithy4s.schema

import smithy4s.Hints

class PartiallyAppliedUnion[U](val alts: Vector[Alt[U, _]]) extends AnyVal {

  def apply(f: U => Int): Schema.UnionSchema[U] =
    Schema.UnionSchema(Schema.placeholder, Hints.empty, alts, f)

  // def apply(
  //     dispatch: U => Alt.WithValue[U, _],
  //     dummy: Boolean = false // source-compat since 0.18
  // ): Schema.UnionSchema[U] = {
  //   val indexMap = alts.zipWithIndex.toMap
  //   Schema.UnionSchema(
  //     Schema.placeholder,
  //     Hints.empty,
  //     alts,
  //     (u: U) => indexMap(dispatch(u).alt)
  //   )
  // }

}
