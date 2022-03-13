package smithy4s

package object schema {

  type SchemaField[S, A] = Field[Schema, S, A]
  type SchemaAlt[U, A] = Alt[Schema, U, A]
  type Repr[A] = String

}
