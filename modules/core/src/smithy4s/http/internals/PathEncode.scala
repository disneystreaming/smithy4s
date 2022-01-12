package smithy4s.http.internals

import smithy4s.internals.Hinted

trait PathEncode[A] { self =>
  def encode(sb: StringBuilder, a: A): Unit
  def encodeGreedy(sb: StringBuilder, a: A): Unit

  def contramap[B](from: B => A): PathEncode[B] = new PathEncode[B] {
    def encode(sb: StringBuilder, b: B): Unit = self.encode(sb, from(b))

    def encodeGreedy(sb: StringBuilder, b: B): Unit =
      self.encodeGreedy(sb, from(b))
  }
}

object PathEncode {

  type MaybePathEncode[A] = Option[PathEncode[A]]
  type Make[A] = Hinted[MaybePathEncode, A]

  object Make {
    def from[A](f: A => String): Make[A] = Hinted.static[MaybePathEncode, A] {
      Some {
        new PathEncode[A] {
          def encode(sb: StringBuilder, a: A): Unit = {
            val _ = sb.append(URIEncoderDecoder.encodeOthers(f(a)))
          }
          def encodeGreedy(sb: StringBuilder, a: A): Unit = {
            f(a).split('/').foreach {
              case s if s.isEmpty() => ()
              case s => sb.append('/').append(URIEncoderDecoder.encodeOthers(s))
            }
          }
        }
      }
    }

    def fromToString[A]: Make[A] = from(_.toString)

    def noop[A]: Make[A] = Hinted.static[MaybePathEncode, A](None)
  }

}
