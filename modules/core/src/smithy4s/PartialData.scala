package smithy4s

import scala.collection.immutable.ArraySeq

/**
  * Data holding either the totality of a structure, of a subset of its fields.
  *
  * This can be used to reconcile bits of data that are coming from several locations in a message
  */
private[smithy4s] sealed trait PartialData[A] {
  def map[B](f: A => B): PartialData[B]
}
// format: off
private[smithy4s] object PartialData {
  case class Total[A](a: A) extends PartialData[A] {
    def map[B](f: A => B): PartialData[B] = Total(f(a))
  }
  case class Partial[A](indexes: IndexedSeq[Int], partialData: IndexedSeq[Any], make: IndexedSeq[Any] => A) extends PartialData[A] {
    def map[B](f: A => B): PartialData[B] = Partial(indexes, partialData, make andThen f)
  }

  /**
    * Reconciles bits of partial data (typically retrieved from various parts of a message)
    * into a single piece of data. It is the responsibility of the caller to ensure that
    * the individual pieces can be reconciled into the full data.
    */
  def unsafeReconcile[A](pieces: PartialData[A]*) : A = {
    pieces.collectFirst {
      case Total(a) => a
    }.getOrElse {
      val allPieces = pieces.asInstanceOf[Seq[PartialData.Partial[A]]]
      var totalSize = 0
      allPieces.foreach(totalSize += _.indexes.size)
      val array = Array.fill[Any](totalSize)(null)
      var make : IndexedSeq[Any] => A = null
      allPieces.foreach { case PartialData.Partial(indexes, data, const) =>
        make = const
        var i = 0
        while(i < data.size) {
          array(indexes(i)) = data(i)
          i += 1
        }
      }
      make(ArraySeq.unsafeWrapArray(array))
    }
  }
}
