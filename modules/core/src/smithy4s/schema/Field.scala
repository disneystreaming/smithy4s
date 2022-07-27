/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s
package schema

/**
  * Represents a member of product type (case class)
  */
sealed abstract class Field[F[_], S, A] {
  type T
  def label: String
  def get: S => A
  def instance: F[T]
  def isRequired: Boolean
  def isOptional: Boolean = !isRequired

  def mapK[G[_]](f: F ~> G): Field[G, S, A]

  /**
    * Grabs the instance associated to this field, applying a polymorphic
    * function when the field is optional
    */
  def instanceA(onOptional: Field.ToOptional[F]): F[A]

  /**
    * Fold a field into a value independant of the type indexed by the field.
    */
  def fold[B](folder: Field.Folder[F, S, B]): B

  /**
    * Fold a field into a value tied to the type indexed by the field.
    */
  def foldK[G[_]](folder: Field.FolderK[F, S, G]): G[A]

  /**
    * Transforms the field into a function that can be applied on the product type,
    * depending on whether it contains the corresponding value.
    */
  def leftFolder[B](folder: Field.LeftFolder[F, B]): (B, S) => B

  /**
    * Applies a side-effecting thunk on the field's value, unpacking
    * the option in case of an optional field.
    */
  def foreachT(s: S)(f: T => Unit): Unit

  /**
    * Applies a side-effecting thunk on the field's value,
    * which will be an option in case of an optional field.
    */
  final def foreachA(s: S)(f: A => Unit): Unit =
    f(this.get(s))

}

object Field {

  def required[F[_], S, A](
      label: String,
      instance: F[A],
      get: S => A
  ): Field[F, S, A] =
    Required(label, instance, get)

  def optional[F[_], S, A](
      label: String,
      instance: F[A],
      get: S => Option[A]
  ): Field[F, S, Option[A]] =
    Optional(label, instance, get)

  private final case class Required[F[_], S, A](
      label: String,
      instance: F[A],
      get: S => A
  ) extends Field[F, S, A] {
    type T = A
    override def toString(): String = s"Required($label, ...)"
    override def mapK[G[_]](fk: F ~> G): Field[G, S, A] =
      Required(label, fk(instance), get)
    override def instanceA(onOptional: ToOptional[F]): F[A] = instance
    override def fold[B](folder: Field.Folder[F, S, B]): B =
      folder.onRequired(label, instance, get)
    override def foldK[G[_]](folder: Field.FolderK[F, S, G]): G[A] =
      folder.onRequired(label, instance, get)
    override def leftFolder[B](folder: Field.LeftFolder[F, B]): (B, S) => B = {
      val partiallyApplied = folder.compile(label, instance)
      (b, s) => partiallyApplied(b, get(s))
    }
    override def isRequired: Boolean = true
    override def foreachT(s: S)(f: A => Unit): Unit = f(get(s))
  }

  private final case class Optional[F[_], S, A](
      label: String,
      instance: F[A],
      get: S => Option[A]
  ) extends Field[F, S, Option[A]] {
    type T = A
    override def toString = s"Optional($label, ...)"
    override def mapK[G[_]](fk: F ~> G): Field[G, S, Option[A]] =
      Optional(label, fk(instance), get)
    override def instanceA(onOptional: ToOptional[F]): F[Option[A]] =
      onOptional.apply(instance)
    override def fold[B](folder: Field.Folder[F, S, B]): B =
      folder.onOptional(label, instance, get)
    override def foldK[G[_]](folder: Field.FolderK[F, S, G]): G[Option[A]] =
      folder.onOptional(label, instance, get)
    override def leftFolder[B](folder: Field.LeftFolder[F, B]): (B, S) => B = {
      val partiallyApplied = folder.compile(label, instance)
      (b, s) =>
        get(s) match {
          case Some(a) => partiallyApplied(b, a)
          case None    => b
        }
    }
    override def isRequired: Boolean = false
    override def foreachT(s: S)(f: A => Unit): Unit = get(s).foreach(f)
  }


  // format: off
  trait FolderK[F[_], S, G[_]]{
    def onRequired[A](label: String, instance: F[A], get: S => A): G[A]
    def onOptional[A](label: String, instance: F[A], get: S => Option[A]): G[Option[A]]
  }

  trait Folder[F[_], S, B] {
    def onRequired[A](label: String, instance: F[A], get: S => A): B
    def onOptional[A](label: String, instance: F[A], get: S => Option[A]): B
  }

  trait LeftFolder[F[_], B] {
    def compile[T](label: String, instance: F[T]) : (B, T) => B
  }

  type Wrapped[F[_], G[_], A] = F[G[A]]
  type ToOptional[F[_]] = PolyFunction[F, Wrapped[F, Option, *]]

  // format: on

  implicit class SchemaFieldOps[S, A](private val field: SchemaField[S, A])
      extends AnyVal {

    def hints: Hints = field.instance.hints

    def addHints(hints: Hint*): SchemaField[S, A] = field.mapK[Schema] {
      new (Schema ~> Schema) {
        def apply[AA](fa: Schema[AA]): Schema[AA] =
          fa.addHints(hints: _*)
      }
    }

    def validated[C, A2](c: C)(implicit
        FieldValidator: Field.FieldValidator[C, A, A2],
        constraint: RefinementProvider.Simple[C, A2]
    ): SchemaField[S, A] = FieldValidator(c, field, constraint)
  }

  /**
   * Construct aiming at facilitate the application of Refinements
   * on fields, as it's made tricky due to the GADT nature of fields.
   */
  trait FieldValidator[C, A, T] {
    def apply[S](
        c: C,
        field: SchemaField[S, A],
        constraint: RefinementProvider.Simple[C, T]
    ): SchemaField[S, A]
  }

  object FieldValidator {
    implicit def requiredCase[C, A]: FieldValidator[C, A, A] =
      new FieldValidator[C, A, A] {
        def apply[S](
            c: C,
            field: SchemaField[S, A],
            constraint: RefinementProvider.Simple[C, A]
        ): SchemaField[S, A] = field match {
          case Required(label, instance, get) =>
            Required(
              label,
              instance.validated(c)(constraint),
              get
            )
          case other => other
        }
      }

    implicit def optionalCase[C, A]: FieldValidator[C, Option[A], A] =
      new FieldValidator[C, Option[A], A] {
        def apply[S](
            c: C,
            field: SchemaField[S, Option[A]],
            constraint: RefinementProvider.Simple[C, A]
        ): SchemaField[S, Option[A]] = field match {
          case opt: Optional[Schema, S, A] =>
            Optional(
              opt.label,
              opt.instance.validated(c)(constraint),
              opt.get
            )
          case other => other
        }
      }
  }

}
