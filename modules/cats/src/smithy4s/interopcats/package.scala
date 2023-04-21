package smithy4s


import scala.annotation.tailrec
import scala.util.hashing.MurmurHash3

package object interopcats {

  def combineHash(start: Int, hashes:Int*): Int = {
    @tailrec
    def loop(hashes: List[Int], acc: Int, length:Int): Int = hashes match {
      case Nil =>   MurmurHash3.finalizeHash(acc, length)
      case h :: t => loop(t, MurmurHash3.mix(acc, h),length + 1)
    }
    loop(hashes.toList, start, 1)
  }
}
