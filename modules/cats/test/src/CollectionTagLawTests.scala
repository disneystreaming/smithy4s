/*
 *  Copyright 2021-2025 Disney Streaming
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

package smithy4s.interopcats

import alleycats.laws.discipline._
import cats._
import cats.kernel.CommutativeGroup
import cats.laws.discipline.arbitrary._
import cats.laws.discipline._
import cats.syntax.all._
import weaver._
import weaver.discipline._
import smithy4s.schema.CollectionTag

object CollectionTagLawTests extends FunSuite with Discipline {
  private implicit def eqIndexedSeq[A: Eq]: Eq[IndexedSeq[A]] =
    Eq.instance(_.zip(_).forall { case (a, b) => a === b })

  private implicit def eqSet[A: Eq]: Eq[Set[A]] = Eq.instance(_.zip(_).forall {
    case (a, b) => a === b
  })

  private implicit val miniIntAddition: CommutativeGroup[MiniInt] =
    MiniInt.miniIntAddition

  private implicit val list: CollectionTag[List] = CollectionTag.ListTag
  private implicit val vector: CollectionTag[Vector] = CollectionTag.VectorTag
  private implicit val indexedSeq: CollectionTag[IndexedSeq] =
    CollectionTag.IndexedSeqTag
  private implicit val set: CollectionTag[Set] = CollectionTag.SetTag

  // List and Vector are fully lawful
  checkAll(
    "Monad[List] via CollectionTag.ListTag",
    MonadTests(catsInstancesGivenCollectionTag[List])
      .monad[MiniInt, MiniInt, MiniInt]
  )
  checkAll(
    "Traverse[List] via CollectionTag.ListTag",
    TraverseTests(catsInstancesGivenCollectionTag[List])
      .traverse[MiniInt, MiniInt, MiniInt, MiniInt, Option, Option]
  )
  checkAll(
    "Alternative[List] via CollectionTag.ListTag",
    AlternativeTests(catsInstancesGivenCollectionTag[List])
      .alternative[MiniInt, MiniInt, MiniInt]
  )
  checkAll(
    "CoflatMap[List] via CollectionTag.ListTag",
    CoflatMapTests(catsInstancesGivenCollectionTag[List])
      .coflatMap[MiniInt, MiniInt, MiniInt]
  )
  checkAll(
    "Align[List] via CollectionTag.ListTag",
    AlignTests(catsInstancesGivenCollectionTag[List])
      .align[MiniInt, MiniInt, MiniInt, MiniInt]
  )

  checkAll(
    "Monad[Vector] via CollectionTag.VectorTag",
    MonadTests(catsInstancesGivenCollectionTag[Vector])
      .monad[MiniInt, MiniInt, MiniInt]
  )
  checkAll(
    "Traverse[Vector] via CollectionTag.VectorTag",
    TraverseTests(catsInstancesGivenCollectionTag[Vector])
      .traverse[MiniInt, MiniInt, MiniInt, MiniInt, Option, Option]
  )
  checkAll(
    "Alternative[Vector] via CollectionTag.VectorTag",
    AlternativeTests(catsInstancesGivenCollectionTag[Vector])
      .alternative[MiniInt, MiniInt, MiniInt]
  )
  checkAll(
    "CoflatMap[Vector] via CollectionTag.VectorTag",
    CoflatMapTests(catsInstancesGivenCollectionTag[Vector])
      .coflatMap[MiniInt, MiniInt, MiniInt]
  )
  checkAll(
    "Align[Vector] via CollectionTag.VectorTag",
    AlignTests(catsInstancesGivenCollectionTag[Vector])
      .align[MiniInt, MiniInt, MiniInt, MiniInt]
  )

  checkAll(
    "Monad[IndexedSeq] via CollectionTag.IndexedSeqTag",
    MonadTests(catsInstancesGivenCollectionTag[IndexedSeq])
      .monad[MiniInt, MiniInt, MiniInt]
  )
  checkAll(
    "Traverse[IndexedSeq] via CollectionTag.IndexedSeqTag",
    TraverseTests(catsInstancesGivenCollectionTag[IndexedSeq])
      .unorderedTraverse[MiniInt, MiniInt, MiniInt, Option, Option]
  )
  checkAll(
    "Alternative[IndexedSeq] via CollectionTag.IndexedSeqTag",
    AlternativeTests(catsInstancesGivenCollectionTag[IndexedSeq])
      .alternative[MiniInt, MiniInt, MiniInt]
  )
  checkAll(
    "CoflatMap[IndexedSeq] via CollectionTag.IndexedSeqTag",
    CoflatMapTests(catsInstancesGivenCollectionTag[IndexedSeq])
      .coflatMap[MiniInt, MiniInt, MiniInt]
  )
  checkAll(
    "Align[IndexedSeq] via CollectionTag.IndexedSeqTag",
    AlignTests(catsInstancesGivenCollectionTag[IndexedSeq])
      .align[MiniInt, MiniInt, MiniInt, MiniInt]
  )

  // Monad[Set] is not fully lawful (see comments in alleycats) but
  // it's mostly lawful and definitely useful
  checkAll(
    "FlatMapRecTests[Set] via CollectionTag.SetTag",
    FlatMapRecTests(catsInstancesGivenCollectionTagForSet[Set])
      .tailRecM[MiniInt]
  )
  checkAll(
    "Traverse[Set] via CollectionTag.SetTag",
    TraverseTests(catsInstancesGivenCollectionTagForSet[Set])
      .unorderedTraverse[MiniInt, MiniInt, MiniInt, Option, Option]
  )

}
