package smithy4s.schema

import smithy4s.Hints

class PartiallyAppliedUnion[U](val alts: Vector[Alt[U, _]]) extends AnyVal {

  def apply(f: U => Int): Schema.UnionSchema[U] =
    Schema.UnionSchema(Schema.placeholder, Hints.empty, alts, f)

}
