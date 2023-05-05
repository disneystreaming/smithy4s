package smithy4s.example.typeclass

import smithy4s.schema._
import cats.kernel.Eq

object EqInterpreter extends CachedSchemaCompiler.Impl[Eq] {

  protected type Aux[A] = Eq[A]

  def fromSchema[A](
      schema: Schema[A],
      cache: Cache
  ): Eq[A] = {
    // Discarding the schema for the sake of the example.
    // Also, the generated code aims at being sensical from
    // the perspective of universalEquals
    Eq.fromUniversalEquals[A]
  }

}
