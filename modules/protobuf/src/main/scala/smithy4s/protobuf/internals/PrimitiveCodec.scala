package smithy4s.protobuf.internals

import com.google.protobuf.CodedOutputStream
import com.google.protobuf.CodedInputStream

private[internals] trait PrimitiveCodec[A] { self =>
  def wireType: Int

  def isZero(a: A): Boolean
  def zero: A
  def sizeNoTag(a: A): Int
  def sizeTag(tag: Int, a: A): Int
  def writeNoTag(a: A, os: CodedOutputStream): Unit
  def writeTag(tag: Int, a: A, os: CodedOutputStream): Unit
  def read(is: CodedInputStream): A
}
