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

package smithy4s.protobuf.internals

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import smithy4s.Bijection
import smithy4s.Lazy
import smithy4s.protobuf.internals.TaggedCodec.FieldTags.OneOf
import smithy4s.protobuf.internals.TaggedCodec.FieldTags.Simple
import smithy4s.schema.CollectionTag

import scala.collection.mutable.Buffer

import TaggedCodec._
import smithy4s.protobuf.ProtobufReadError
import smithy4s.ShapeId

private[protobuf] sealed trait TaggedCodec[A] {
  def wireType: Int
  def isPrimitive: Boolean
  def isMessage: Boolean
  def prepareWrite(protoIndex: Int, a: A): WriteNode
  def prepareNonEmptyWrite(protoIndex: Int, a: A): WriteNode
  def prepareRead(): ReadNode[A]

  final def imap[B](biject: Bijection[A, B]): TaggedCodec[B] =
    TaggedCodec.IMapCodec(this, biject.to, biject.from)

  final def imap[B](to: A => B, from: B => A): TaggedCodec[B] = this match {
    case IMapCodec(underlying, to_, from_) =>
      IMapCodec(underlying, to_.andThen(to), from.andThen(from_))
    case other => IMapCodec(other, to, from)
  }

  // Adds a layer of message, where the field is indexed by 1.
  final def wrap: MessageCodec[A] = {
    val fieldTags = oneOfTags match {
      case Some(tags) => FieldTags.OneOf(tags)
      case None       => FieldTags.Simple(1)
    }
    MessageCodec(
      IndexedSeq(FieldCodec(fieldTags, this, identity[A])),
      seq => seq(0).asInstanceOf[A]
    )
  }

  def oneOfTags: Option[Seq[Int]]

}

private[protobuf] object TaggedCodec {

  final case class ScalarFieldCodec[A](
      underlying: ScalarCodec[A]
  ) extends TaggedCodec[A] {
    def wireType: Int = underlying.wireType
    def isPrimitive: Boolean = true
    def isMessage: Boolean = false
    def oneOfTags: Option[Seq[Int]] = None
    def write(protoIndex: Int, a: A, os: CodedOutputStream): Unit =
      underlying.writeTag(protoIndex, a, os)
    def prepareWrite(protoIndex: Int, a: A): WriteNode = {
      if (underlying.isZero(a)) {
        WriteNode.empty
      } else prepareNonEmptyWrite(protoIndex, a)
    }
    def prepareNonEmptyWrite(protoIndex: Int, a: A): WriteNode = {
      new WriteNode(underlying.sizeTag(protoIndex, a)) {
        def write(os: CodedOutputStream): Unit =
          underlying.writeTag(protoIndex, a, os)
      }
    }
    override def prepareRead(): ReadNode[A] = new ReadNode[A] {
      var wasRead: Boolean = false
      var value: A = underlying.zero
      def readOne(is: CodedInputStream): Unit = {
        value = underlying.read(is)
        wasRead = true
      }
      def complete(): A = value
      def readOne(tag: Int, is: CodedInputStream): Unit = readOne(is)
    }
  }

  final case class NonScalarPrimitiveFieldCodec[A](
      underlying: NonScalarPrimitiveCodec[A]
  ) extends TaggedCodec[A] {
    def wireType: Int = underlying.wireType
    def isPrimitive: Boolean = true
    def isMessage: Boolean = false
    def oneOfTags: Option[Seq[Int]] = None

    def prepareWrite(protoIndex: Int, a: A): WriteNode = {
      if (underlying.isZero(a)) {
        WriteNode.empty
      } else prepareNonEmptyWrite(protoIndex, a)
    }

    def prepareNonEmptyWrite(protoIndex: Int, a: A): WriteNode = {
      new WriteNode(underlying.sizeTag(protoIndex, a)) {
        def write(os: CodedOutputStream): Unit =
          underlying.writeTag(protoIndex, a, os)
      }
    }
    override def prepareRead(): ReadNode[A] = new ReadNode[A] {
      var wasRead: Boolean = false
      var value: A = underlying.zero
      def readOne(is: CodedInputStream): Unit = {
        value = underlying.read(is)
        wasRead = true
      }
      def readOne(tag: Int, is: CodedInputStream): Unit = readOne(is)
      def complete(): A = value
    }
  }

  final case class PackedRepeatedFieldCodec[C[_], A](
      scalarCodec: ScalarCodec[A],
      collectionTag: CollectionTag[C]
  ) extends TaggedCodec[C[A]] {
    def isPrimitive: Boolean = false
    def isMessage: Boolean = false
    def wireType: Int = Wire.WireType.LengthDelimited
    def oneOfTags: Option[Seq[Int]] = None

    def prepareWrite(protoIndex: Int, ca: C[A]): WriteNode =
      if (collectionTag.isEmpty(ca)) {
        WriteNode.empty
      } else prepareNonEmptyWrite(protoIndex, ca)

    def prepareNonEmptyWrite(protoIndex: Int, ca: C[A]): WriteNode = {
      var payloadSize = 0
      collectionTag
        .iterator(ca)
        .foreach(payloadSize += scalarCodec.sizeNoTag(_))
      val size = {
        val sizeOfPayloadSizeTag =
          CodedOutputStream.computeUInt32SizeNoTag(payloadSize)
        // a number of bytes for the tag
        // however many bytes for encoding the size of the payload (as a varint)
        // See https://developers.google.com/protocol-buffers/docs/encoding#packed
        val tagSize = CodedOutputStream.computeTagSize(protoIndex)
        tagSize + sizeOfPayloadSizeTag + payloadSize
      }
      new WriteNode(size) {
        def write(os: CodedOutputStream): Unit = {
          os.writeTag(protoIndex, 2)
          os.writeUInt32NoTag(payloadSize)
          collectionTag.iterator(ca).foreach(scalarCodec.writeNoTag(_, os))
        }
      }
    }

    def prepareRead(): ReadNode[C[A]] = new ReadNode[C[A]] {
      var wasRead: Boolean = false
      val buffer = Buffer.empty[A]
      def readOne(is: CodedInputStream): Unit = {
        val messageLength = is.readRawVarint32()
        val oldLimit = is.pushLimit(messageLength)
        while (is.getBytesUntilLimit > 0) {
          buffer += scalarCodec.read(is)
        }
        is.popLimit(oldLimit)
        wasRead = true
      }
      def readOne(tag: Int, is: CodedInputStream): Unit = readOne(is)
      def complete(): C[A] = collectionTag.fromIterator(buffer.iterator)
    }
  }

  final case class UnpackedRepeatedFieldCodec[C[_], A](
      codec: TaggedCodec[A],
      collectionTag: CollectionTag[C]
  ) extends TaggedCodec[C[A]] {
    def wireType: Int = codec.wireType
    def isPrimitive: Boolean = false
    def isMessage: Boolean = false
    def oneOfTags: Option[Seq[Int]] = None

    def prepareWrite(protoIndex: Int, ca: C[A]): WriteNode =
      if (collectionTag.isEmpty(ca)) {
        WriteNode.empty
      } else prepareNonEmptyWrite(protoIndex, ca)

    def prepareNonEmptyWrite(protoIndex: Int, ca: C[A]): WriteNode = {
      val aNodes =
        collectionTag
          .iterator(ca)
          .map(codec.prepareWrite(protoIndex, _))
          .toVector
      var serialisedSize = 0 // scalafix:ok
      aNodes.foreach(serialisedSize += _.serialisedSize)
      new WriteNode(serialisedSize) {
        override def write(os: CodedOutputStream): Unit =
          aNodes.foreach(_.write(os))
      }
    }

    def prepareRead(): ReadNode[C[A]] = new ReadNode[C[A]] {
      var wasRead: Boolean = false
      val buffer = Buffer.empty[A]
      def readOne(is: CodedInputStream): Unit = {
        // dynamically creating a node as re-using
        // may be dangerous in case of messages.
        // an alternative could be to have a `def resetState()` method.
        val underlyingNode = codec.prepareRead()
        buffer += {
          underlyingNode.readOne(is)
          underlyingNode.complete()
        }
        // at least one element was read
        wasRead = true
      }
      def readOne(tag: Int, is: CodedInputStream): Unit = readOne(is)
      def complete(): C[A] = collectionTag.fromIterator(buffer.iterator)
    }
  }

  sealed trait FieldTags

  object FieldTags {
    case class Simple(protoIndex: Int) extends FieldTags
    case class OneOf(tags: Seq[Int]) extends FieldTags
  }

  final case class FieldCodec[S, A](
      fieldTags: FieldTags,
      codec: TaggedCodec[A],
      get: S => A
  ) {
    def readTags: Seq[Int] = fieldTags match {
      case FieldTags.Simple(protoIndex) =>
        Seq(Wire.encodeTag(protoIndex, codec.wireType))
      case OneOf(tags) => tags
    }

    // In case of OneOf, we're passing a dummy value as it gets discarded anyway.
    private val writeTag = fieldTags match {
      case Simple(protoIndex) => protoIndex
      case OneOf(_)           => 0
    }

    def prepareWrite(s: S): WriteNode =
      codec.prepareWrite(writeTag, get(s))

  }

  final case class MessageCodec[A](
      codecs: IndexedSeq[FieldCodec[A, _]],
      make: IndexedSeq[Any] => A
  ) extends TaggedCodec[A] {

    def isPrimitive: Boolean = false
    def isMessage: Boolean = true
    def wireType: Int = Wire.WireType.LengthDelimited
    def oneOfTags: Option[Seq[Int]] = None

    /** Prepares a Node that doesn't serialise a tag nor the size, thus can be
      * used to serialise top-level messages.
      */
    def prepareWriteNoTag(a: A): WriteNode = {
      val allNodes = codecs.map(_.prepareWrite(a))
      var payloadSize = 0
      allNodes.foreach(n => payloadSize += n.serialisedSize)
      new WriteNode(payloadSize) {
        def write(os: CodedOutputStream): Unit = {
          allNodes.foreach(_.write(os))
        }
      }
    }

    def prepareNonEmptyWrite(protoIndex: Int, a: A): WriteNode =
      prepareWrite(protoIndex, a)

    def prepareWrite(protoIndex: Int, a: A): WriteNode = {
      val allNodes = codecs.map(_.prepareWrite(a))
      var payloadSize = 0
      allNodes.foreach(n => payloadSize += n.serialisedSize)
      val sizeOfPayloadSizeTag =
        CodedOutputStream.computeUInt32SizeNoTag(payloadSize)
      val tagSize = CodedOutputStream.computeTagSize(protoIndex)
      val size = tagSize + sizeOfPayloadSizeTag + payloadSize
      new WriteNode(size) {
        def write(os: CodedOutputStream): Unit = {
          os.writeTag(protoIndex, 2)
          os.writeUInt32NoTag(payloadSize)
          allNodes.foreach(_.write(os))
        }
      }
    }

    override def prepareRead(): ReadNode[A] = new ReadNode[A] {
      private val noTag = prepareReadNoTag()
      def wasRead: Boolean = noTag.wasRead

      def readOne(is: CodedInputStream): Unit = {
        val messageLength = is.readRawVarint32()
        val oldLimit = is.pushLimit(messageLength)
        noTag.readOne(is)
        is.popLimit(oldLimit)
      }

      def readOne(tag: Int, is: CodedInputStream): Unit = readOne(is)

      def complete(): A = noTag.complete()
    }

    def prepareReadNoTag(): ReadNode[A] = new ReadNode[A] {
      val nodeMap = scala.collection.mutable.LinkedHashMap[Int, ReadNode[_]]()
      val nodeList = Buffer.empty[ReadNode[_]]

      codecs.foreach { case field: FieldCodec[A, _] =>
        val readNode = field.codec.prepareRead()
        nodeList.append(readNode)
        field.readTags.foreach { tag =>
          nodeMap.put(tag, readNode)
        }
      }

      var wasRead: Boolean = false

      def readOne(is: CodedInputStream): Unit = {
        var done = false
        wasRead = true
        while (!done) {
          val currentTag = is.readTag()
          if (currentTag == 0) done = true
          else {
            nodeMap.get(currentTag) match {
              case Some(readNode) => {
                readNode.readOne(currentTag, is)
              }
              case None => is.skipField(currentTag)
            }
          }
        }
      }

      def readOne(tag: Int, is: CodedInputStream): Unit = readOne(is)

      def complete(): A = {
        val buffer = IndexedSeq.newBuilder[Any]
        nodeList.foreach { node =>
          buffer += node.complete()
        }
        make(buffer.result())
      }
    }
  }

  final case class OneOfAlternative[U, A](
      protoIndex: Int,
      codec: TaggedCodec[A],
      inject: A => U,
      project: PartialFunction[U, A]
  ) {
    def tag = Wire.encodeTag(protoIndex, codec.wireType)
    def unsafePrepareWrite(u: U): WriteNode = {
      codec.prepareNonEmptyWrite(protoIndex, project(u))
    }
  }

  final case class OneOfCodec[U](
      alts: IndexedSeq[OneOfAlternative[U, _]],
      ordinal: U => Int
  ) extends TaggedCodec[U] {

    override def wireType: Int = throw new IllegalAccessError("Coding error")

    override def prepareWrite(protoIndex: Int, u: U): WriteNode = {
      // Disregarding the protoIndex received as the information is inherent
      // to which instance of `U` is received
      val index = ordinal(u)
      alts(index).unsafePrepareWrite(u)
    }

    def prepareNonEmptyWrite(protoIndex: Int, u: U): WriteNode =
      prepareWrite(protoIndex, u)

    def oneOfTags: Option[Seq[Int]] = Some(alts.map(_.tag).toList)

    def isPrimitive: Boolean = false
    def isMessage: Boolean = false

    def prepareWrite(u: U): WriteNode = {
      val index = ordinal(u)
      alts(index).unsafePrepareWrite(u)
    }

    val nodeMap: Map[Int, () => ReadNode[U]] = alts.map {
      case alt: OneOfAlternative[U, a] =>
        alt.tag -> (() => alt.codec.prepareRead().map(alt.inject))
    }.toMap

    def prepareRead(): ReadNode[U] = new ReadNode[U] {
      var node: ReadNode[U] = null
      var wasRead = false
      def readOne(tag: Int, is: CodedInputStream): Unit = {
        if (node == null) {
          node = nodeMap(tag).apply()
        }
        node.readOne(is)
        wasRead = true
      }
      def complete(): U = node.complete()
      def readOne(is: CodedInputStream): Unit =
        throw new IllegalAccessError("Coding error")
    }
  }

  final case class IMapCodec[A, B](
      underlying: TaggedCodec[A],
      to: A => B,
      from: B => A
  ) extends TaggedCodec[B] {
    def isPrimitive: Boolean = underlying.isPrimitive
    def isMessage: Boolean = underlying.isMessage
    def wireType: Int = underlying.wireType
    def oneOfTags: Option[Seq[Int]] = underlying.oneOfTags

    def prepareWrite(protoIndex: Int, b: B): WriteNode =
      underlying.prepareWrite(protoIndex, from(b))

    def prepareNonEmptyWrite(protoIndex: Int, b: B): WriteNode =
      underlying.prepareNonEmptyWrite(protoIndex, from(b))

    def prepareRead(): ReadNode[B] = underlying.prepareRead().map(to)
  }

  final case class OptionCodec[A](underlying: TaggedCodec[A])
      extends TaggedCodec[Option[A]] {
    def isPrimitive: Boolean = underlying.isPrimitive
    def isMessage: Boolean = underlying.isMessage
    def wireType = underlying.wireType
    def oneOfTags: Option[Seq[Int]] = underlying.oneOfTags

    def prepareWrite(protoIndex: Int, a: Option[A]): WriteNode = a match {
      case None        => WriteNode.empty
      case Some(value) => underlying.prepareWrite(protoIndex: Int, value)
    }

    def prepareNonEmptyWrite(protoIndex: Int, a: Option[A]): WriteNode =
      prepareWrite(protoIndex, a)

    override def prepareRead(): ReadNode[Option[A]] = new ReadNode[Option[A]] {
      val readA = underlying.prepareRead()
      var wasRead: Boolean = false
      def readOne(is: CodedInputStream): Unit = {
        readA.readOne(is)
        wasRead = readA.wasRead
      }
      def readOne(tag: Int, is: CodedInputStream): Unit = {
        readA.readOne(tag, is)
        wasRead = readA.wasRead
      }
      def complete(): Option[A] = if (wasRead) Some(readA.complete()) else None
    }
  }

  final case class StrictlyRequiredCodec[A](
      shapeId: ShapeId,
      field: String,
      protoIndex: Int,
      underlying: TaggedCodec[A]
  ) extends TaggedCodec[A] {
    def isPrimitive: Boolean = underlying.isPrimitive
    def isMessage: Boolean = underlying.isMessage
    def wireType = underlying.wireType
    def oneOfTags: Option[Seq[Int]] = underlying.oneOfTags

    def prepareWrite(protoIndex: Int, a: A): WriteNode =
      underlying.prepareWrite(protoIndex: Int, a)

    def prepareNonEmptyWrite(protoIndex: Int, a: A): WriteNode =
      prepareWrite(protoIndex, a)

    override def prepareRead(): ReadNode[A] = new ReadNode[A] {
      val readA = underlying.prepareRead()
      var wasRead: Boolean = false
      def readOne(is: CodedInputStream): Unit = {
        readA.readOne(is)
        wasRead = readA.wasRead
      }
      def readOne(tag: Int, is: CodedInputStream): Unit = {
        readA.readOne(tag, is)
        wasRead = readA.wasRead
      }
      def complete(): A = if (wasRead) readA.complete()
      else
        throw ProtobufReadError.MissingRequiredField(shapeId, field, protoIndex)
    }
  }

  final case class RecursiveCodec[A](suspended: Lazy[TaggedCodec[A]])
      extends TaggedCodec[A] {
    def isPrimitive: Boolean = false
    def isMessage: Boolean = true
    def wireType = suspended.value.wireType
    def oneOfTags: Option[Seq[Int]] = None

    lazy val underlying = suspended.value
    def prepareWrite(protoIndex: Int, a: A): WriteNode =
      underlying.prepareWrite(protoIndex, a)

    def prepareNonEmptyWrite(protoIndex: Int, a: A): WriteNode =
      underlying.prepareNonEmptyWrite(protoIndex, a)

    def prepareRead(): ReadNode[A] = new ReadNode[A] {
      // not using a lazy val because the logic that exercises
      // this is inherently single-threaded
      var lazyNodeValue: ReadNode[A] = null
      var wasRead: Boolean = false

      def readOne(is: CodedInputStream): Unit = {
        if (lazyNodeValue == null) {
          lazyNodeValue = underlying.prepareRead()
        }
        lazyNodeValue.readOne(is)
        wasRead = true
      }

      def readOne(tag: Int, is: CodedInputStream): Unit = {
        if (lazyNodeValue == null) {
          lazyNodeValue = underlying.prepareRead()
        }
        lazyNodeValue.readOne(tag, is)
        wasRead = true
      }

      def complete(): A = {
        if (lazyNodeValue == null) {
          lazyNodeValue = underlying.prepareRead()
        }
        lazyNodeValue.complete()
      }
    }
  }

  final case class ClosedEnumerationCodec[E](default: E, rest: List[(Int, E)])
      extends TaggedCodec[E] {
    def wireType: Int = Wire.WireType.Varint
    def isPrimitive: Boolean = true
    def isMessage: Boolean = false

    val enumToIntMap = rest.map(_.swap).toMap + (default -> 0)
    val intToEnumMap = rest.toMap

    def prepareWrite(protoIndex: Int, enumValue: E): WriteNode = {
      val protoIntValue = enumToIntMap(enumValue)
      if (protoIntValue == 0) {
        WriteNode.empty
      } else {
        new WriteNode(ScalarCodec.IntCodec.sizeTag(protoIndex, protoIntValue)) {
          def write(os: CodedOutputStream): Unit = {
            ScalarCodec.IntCodec.writeTag(protoIndex, protoIntValue, os)
          }
        }
      }
    }

    def prepareNonEmptyWrite(protoIndex: Int, enumValue: E): WriteNode = {
      val protoIntValue = enumToIntMap(enumValue)

      new WriteNode(ScalarCodec.IntCodec.sizeTag(protoIndex, protoIntValue)) {
        def write(os: CodedOutputStream): Unit = {
          ScalarCodec.IntCodec.writeTag(protoIndex, protoIntValue, os)
        }
      }
    }

    def prepareRead(): ReadNode[E] = new ReadNode[E] {
      var wasRead: Boolean = false
      var result: Int = 0
      def readOne(is: CodedInputStream): Unit = {
        result = is.readInt32()
        wasRead = true
      }
      def readOne(tag: Int, is: CodedInputStream): Unit = readOne(is)
      def complete(): E = {
        intToEnumMap.getOrElse(result, default)
      }
    }
    def oneOfTags: Option[Seq[Int]] = None
  }

}
