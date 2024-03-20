package smithy4s.protobuf.internals

import com.google.protobuf.CodedOutputStream

/** Unfortunately, the process of serialisation requires us to store the
  * serialised size somewhere to avoid recomputing it, as it's involved not only
  * in the computation of the size of the whole message, but also as a local
  * header in case of length-delimited bits.
  *
  * The `ProtoNode` is a meant to store this size as well as the function that
  * writes a field to a protobuf-specialised output stream.
  *
  * ScalaPB (and probably other protobuf libraries) manages to avoid this extra
  * allocation by having mutable variables in the generated case-classes to
  * store this information, thus evading the extra allocation at the cost of
  * highly specialised data models.
  */
private[internals] abstract class WriteNode(val serialisedSize: Int) {
  def write(os: CodedOutputStream): Unit
}

object WriteNode {

  val empty: WriteNode = new WriteNode(0) {
    def write(os: CodedOutputStream): Unit = ()
  }

}
