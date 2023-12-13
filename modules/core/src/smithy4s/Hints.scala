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

  def lazily(underlying: => Hints): Hints = Hints.LazyHints(Lazy(underlying))

  def member(bindings: Hint*): Hints =
    Impl(memberHintsMap = mapFromSeq(bindings), targetHintsMap = Map.empty)

  def fromSeq(bindings: Seq[Hint]): Hints =
    Impl(memberHintsMap = Map.empty, targetHintsMap = mapFromSeq(bindings))

  private def mapFromSeq(bindings: Seq[Hint]): Map[ShapeId, Hint] = {
    bindings.map {
      case b @ Binding.StaticBinding(k, _)  => k.id -> b
      case b @ Binding.DynamicBinding(k, _) => k -> b
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
        case Binding.StaticBinding(k, value) =>
          if (key.eq(k)) Some(value.asInstanceOf[A]) else None
        case Binding.DynamicBinding(_, value) =>
          Document.Decoder.fromSchema(key.schema).decode(value).toOption
      }
    def ++(other: Hints): Hints = concat(this, other)

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

  private[smithy4s] final case class LazyHints(underlying: Lazy[Hints])
      extends Hints {
    override def isEmpty: Boolean = underlying.value.isEmpty

    override def all: Iterable[Binding] = underlying.value.all

    override def memberHintsMap: Map[ShapeId, Binding] =
      underlying.value.memberHintsMap

    override def targetHintsMap: Map[ShapeId, Binding] =
      underlying.value.targetHintsMap

    override def toMap: Map[ShapeId, Binding] = underlying.value.toMap

    override def get[A](implicit key: ShapeTag[A]): Option[A] =
      underlying.value.get(key)

    override def ++(other: Hints): Hints = concat(this, other)

    override def addMemberHints(hints: Hints): Hints =
      underlying.value.addMemberHints(hints)

    override def addTargetHints(hints: Hints): Hints =
      underlying.value.addTargetHints(hints)

    override def memberHints: Hints = underlying.value.memberHints

    override def targetHints: Hints = underlying.value.targetHints
  }

  private def concat(lhs: Hints, rhs: Hints): Hints = (lhs, rhs) match {
    case (LazyHints(lazyA), _) => LazyHints(Lazy(lazyA.value ++ rhs))
    case (_, LazyHints(lazyB)) => LazyHints(Lazy(lhs ++ lazyB.value))
    case _ => {
      Impl(
        memberHintsMap = lhs.memberHintsMap ++ rhs.memberHintsMap,
        targetHintsMap = lhs.targetHintsMap ++ rhs.targetHintsMap
      )
    }
  }

  sealed trait Binding extends Product with Serializable {
    def keyId: ShapeId
  }

  object Binding {
    final case class StaticBinding[A](key: ShapeTag[A], value: A)
        extends Binding {
      override def keyId: ShapeId = key.id
      override def toString: String = value.toString()
    }
    final case class DynamicBinding(keyId: ShapeId, value: Document)
        extends Binding {
      override def toString = Document.obj(keyId.show -> value).toString()
    }

    implicit def fromValue[A, AA <: A](value: AA)(implicit
        key: ShapeTag[A]
    ): Binding = StaticBinding(key, value)

    implicit def fromTuple(tup: (ShapeId, Document)): Binding =
      DynamicBinding(tup._1, tup._2)
  }

}
