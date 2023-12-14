/*
 *  Copyright 2021-2023 Disney Streaming
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

import java.util.Objects

/**
  * A hint is an arbitrary piece of data that can be added to a schema,
  * at the struct level, or at the field/member level.
  *
  * You can think of it as an annotation that can communicate
  * additional information to encoders/decoders (for instance, a change
  * in a label, a regex pattern some string should abide by, a range, etc)
  *
  * This `Hints` interface is a container for hints.
  *
  * Under the hood, the hints are composed of two maps : one for member-level hints,
  * one for target-level hints. Member-level hints typically hold values corresponding
  * to member traits, whereas target hints hold values corresponding to normal data shapes.
  */
trait Hints {
  def isEmpty: Boolean
  def all: Iterable[Hints.Binding]

  def memberHintsMap: Map[ShapeId, Hints.Binding]
  def targetHintsMap: Map[ShapeId, Hints.Binding]

  /**
    * Returns a map of hints from both level, the member-level having priority
    * over the target-level one.
    */
  def toMap: Map[ShapeId, Hints.Binding]

  def get[A](implicit key: ShapeTag[A]): Option[A]
  final def has[A](implicit key: ShapeTag[A]): Boolean = this.get[A].isDefined
  final def get[A](key: ShapeTag.Has[A]): Option[A] = get(key.getTag)
  final def get[T](nt: Newtype[T]): Option[nt.Type] = get(nt.tag)
  final def filter(predicate: Hint => Boolean): Hints =
    Hints.fromSeq(all.filter(predicate).toSeq)
  final def filterNot(predicate: Hint => Boolean): Hints =
    filter(hint => !predicate(hint))

  /**
    *  Concatenates two set of hints. The levels are concatenated independently.
    */
  def ++(other: Hints): Hints

  /**
    * Add hints to the member-level.
    */
  def addMemberHints(hints: Hints): Hints

  /**
    * Add hints to the member-level.
    */
  final def addMemberHints(hints: Hint*): Hints = addMemberHints(
    Hints(hints: _*)
  )

  /**
   *  Add hints to the member level
   */
  final def add(hints: Hint*): Hints = addMemberHints(hints: _*)

  /**
    * Add hints to the target-level.
    */
  def addTargetHints(hints: Hints): Hints

  /**
    * Add hints to the target-level.
    */
  final def addTargetHints(hints: Hint*): Hints = addTargetHints(
    Hints(hints: _*)
  )

  /**
    * Provides an instance of hints containing only the member-level hints.
    */
  def memberHints: Hints

  /**
    * Provides an instance of hints containing only the target-level hints.
    */
  def targetHints: Hints

  /**
   * Adds a new hint provided a specific hint is present
   */
  final def expand[A, B](f: A => Hint)(implicit key: ShapeTag[A]): Hints =
    get(key) match {
      case Some(a) => addMemberHints(f(a))
      case None    => this
    }

}

object Hints {

  val empty: Hints = new Impl(Map.empty, Map.empty)

  def apply(bindings: Hint*): Hints =
    fromSeq(bindings)

  def member(bindings: Hint*): Hints =
    Impl(memberHintsMap = mapFromSeq(bindings), targetHintsMap = Map.empty)

  def fromSeq(bindings: Seq[Hint]): Hints =
    Impl(memberHintsMap = Map.empty, targetHintsMap = mapFromSeq(bindings))

  private def mapFromSeq(bindings: Seq[Hint]): Map[ShapeId, Hint] = {
    bindings.map {
      case b @ Binding.StaticBinding(k, _)  => k.id -> b
      case b @ Binding.DynamicBinding(k, _) => k -> b
      case null | _                         => sys.error("unreachable")
    }.toMap
  }

  private[smithy4s] final case class Impl(
      memberHintsMap: Map[ShapeId, Hint],
      targetHintsMap: Map[ShapeId, Hint]
  ) extends Hints {
    val toMap = targetHintsMap ++ memberHintsMap
    def isEmpty = toMap.isEmpty
    def all: Iterable[Hint] = toMap.values
    def get[A](implicit key: ShapeTag[A]): Option[A] =
      toMap.get(key.id).flatMap {
        case Binding.StaticBinding.UnapplyFull(k, value) =>
          if (key.eq(k)) Some(value.value.asInstanceOf[A]) else None
        case Binding.DynamicBinding(_, value) =>
          Document.Decoder.fromSchema(key.schema).decode(value).toOption
        // to satisfy the compiler, although this is unreachable
        case null | _ =>
          sys.error("impossible")
      }
    def ++(other: Hints): Hints = {
      Impl(
        memberHintsMap = memberHintsMap ++ other.memberHintsMap,
        targetHintsMap = targetHintsMap ++ other.targetHintsMap
      )
    }
    def targetHints: Hints = Impl(Map.empty, targetHintsMap)
    def memberHints: Hints = Impl(memberHintsMap, Map.empty)
    def addMemberHints(hints: Hints): Hints =
      Impl(
        memberHintsMap = memberHintsMap ++ hints.toMap,
        targetHintsMap = targetHintsMap
      )

    def addTargetHints(hints: Hints): Hints =
      Impl(
        memberHintsMap = memberHintsMap,
        targetHintsMap = targetHintsMap ++ hints.toMap
      )

    override def toString(): String =
      s"Hints(${all.mkString(", ")})"
  }

  sealed trait Binding extends Product with Serializable {
    def keyId: ShapeId
  }

  object Binding {
    final case class StaticBinding[A](
        k: ShapeTag[A],
        private val v: Lazy[A]
    ) extends Binding
        with StaticBindingPlatform[A] {
      override def keyId: ShapeId = key.id
      def key: ShapeTag[A] = k
      def value: A = v.value

      override def toString: String = value.toString()

      override def equals(that: Any): Boolean = that match {
        case rhs: StaticBinding[_] =>
          Objects.equals(this.key, rhs.key) && Objects.equals(
            this.value,
            rhs.value
          )
        case _ => false
      }

      override def hashCode(): Int =
        Objects.hash(key, value.asInstanceOf[AnyRef])

      // BINCOMPAT FOR 0.18 START
      private[Binding] def this(key: ShapeTag[A], value: A) =
        this(key, Lazy(value))

      private[Binding] def copy(
          key: ShapeTag[A],
          value: A
      ): StaticBinding[A] =
        new StaticBinding(key, Lazy(value))

      private[Binding] def copy$default$2(): A = value

      private[Binding] def _1: ShapeTag[A] = key
      private[Binding] def _2: A = value
      // BINCOMPAT FOR 0.18 ENd

    }

    object StaticBinding {
      // BINCOMPAT FOR 0.18 START
      def apply[A](key: ShapeTag[A], value: A): StaticBinding[A] =
        new StaticBinding[A](key, value)

      def unapply[A](
          binding: StaticBinding[A]
      ): UnapplyPolyfill.Result[(ShapeTag[A], A), StaticBinding[A]] =
        UnapplyPolyfill.Result(
          (apply(_: ShapeTag[A], _: A)).tupled,
          (binding.key, binding.value)
        )

      object UnapplyFull {
        def unapply[A <: AnyRef](
            binding: StaticBinding[A]
        ): Some[(ShapeTag[A], Lazy[A])] =
          Some((binding.key, binding.v))
      }
      // BINCOMPAT FOR 0.18 END
    }

    final case class DynamicBinding(keyId: ShapeId, value: Document)
        extends Binding {
      override def toString = Document.obj(keyId.show -> value).toString()
    }

    implicit def fromValueLazy[A, AA <: A](value: => AA)(implicit
        key: ShapeTag[A]
    ): Binding = new StaticBinding[A](key, Lazy[A](value))

    private[smithy4s] def fromValue[A, AA <: A](value: AA)(implicit
        key: ShapeTag[A]
    ): Binding = fromValueLazy[A, AA](value)

    implicit def fromTuple(tup: (ShapeId, Document)): Binding =
      DynamicBinding(tup._1, tup._2)
  }

}
