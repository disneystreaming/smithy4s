package smithy4s
package dynamic

case class KeyedSchema[A](schema: Schema[A], hintKey: Hints.Key[A])

case object KeyedSchema {

  implicit def fromKey[A](key: Hints.Key[A])(implicit
      schema: Schema[A]
  ): KeyedSchema[A] =
    KeyedSchema[A](schema, key)

}
