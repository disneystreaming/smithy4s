package smithy4s

package object schema {

  type SchemaField[P, A] = Field[Schema, P, A]
  type SchemaAlt[S, A] = Alt[Schema, S, A]


}
