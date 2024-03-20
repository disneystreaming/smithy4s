package smithy4s.protobuf.internals

import com.google.protobuf.CodedInputStream

private[internals] trait ReadNode[A] { self =>
  def wasRead: Boolean
  def readOne(is: CodedInputStream): Unit
  // A method typically overridden by inlined unions.
  def readOne(tag: Int, is: CodedInputStream): Unit
  def complete(): A

  final def map[B](f: A => B): ReadNode[B] = new ReadNode[B] {
    def wasRead: Boolean = self.wasRead
    def readOne(is: CodedInputStream): Unit = self.readOne(is)
    override def readOne(tag: Int, is: CodedInputStream): Unit =
      self.readOne(tag, is)
    def complete(): B = f(self.complete())
  }

}
