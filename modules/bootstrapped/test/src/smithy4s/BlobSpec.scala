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

import munit._

import java.nio.ByteBuffer
import java.io.ByteArrayOutputStream
import scala.util.Using
class BlobSpec() extends FunSuite {

  test("sameBytesAs works across data structures") {
    assert(Blob("foo").sameBytesAs(Blob("foo".getBytes)))
    assert(Blob("foo").sameBytesAs(Blob(ByteBuffer.wrap("foo".getBytes))))
  }

  test("equals depends on underlying data structure") {
    assert(Blob("foo") != Blob(ByteBuffer.wrap("foo".getBytes)))
    assert(Blob("foo") == Blob("foo"))
    assert(
      Blob(ByteBuffer.wrap("foo".getBytes)) == Blob(
        ByteBuffer.wrap("foo".getBytes)
      )
    )
  }

  test("ByteArrayBlob.hashcode is consistent") {
    def makeBlob(str: String) = Blob(str.getBytes)
    val blob1 = makeBlob("foo")
    val blob2 = makeBlob("foo")
    val blob3 = makeBlob("bar")
    assertEquals(blob1.hashCode, blob2.hashCode)
    assertNotEquals(blob1.hashCode, blob3.hashCode)
  }

  test("ByteBufferBlob.hashcode is consistent") {
    def makeBlob(str: String) = Blob(ByteBuffer.wrap(str.getBytes))
    val blob1 = makeBlob("foo")
    val blob2 = makeBlob("foo")
    val blob3 = makeBlob("bar")
    assertEquals(blob1.hashCode, blob2.hashCode)
    assertNotEquals(blob1.hashCode, blob3.hashCode)
  }

  test("Concat works as expected") {
    val blob = Blob("foo") ++ Blob("bar")
    assertEquals(blob.size, 6)
    assertEquals(blob(2), 'o'.toByte)
    assertEquals(blob(4), 'a'.toByte)
    java.util.Arrays
      .equals(blob.toArray, "foo".getBytes ++ "bar".getBytes())
  }

  val all = List(
    "Queue" -> (Blob("foo") ++ Blob("bar")),
    "Array" -> Blob("foobar"),
    "Buffer" -> Blob(ByteBuffer.wrap("foobar".getBytes()))
  )

  for {
    x <- all
    y <- all
  } {
    test(s"${x._1} and ${y._1} : same bytes") {
      assert(x._2.sameBytesAs(y._2))
    }
  }

  all.foreach { case (name, data) =>
    test(s"$name: size") {
      assertEquals(data.size, 6)
    }

    test(s"$name: index access") {
      assertEquals(data(2), 'o'.toByte)
    }

    test(s"$name: out of bounds access") {
      intercept[IndexOutOfBoundsException] { data(6) }
    }

    test(s"$name: utf8String") {
      assertEquals(data.toUTF8String, "foobar")
    }

    test(s"$name: toArraySliceBlob") {
      assertEquals[Blob, Blob](
        data.toArraySliceBlob,
        Blob.slice("foobar".getBytes(), 0, 6)
      )
    }

    test(s"$name: copyToArray") {
      val target = Array.fill[Byte](6)(0)
      data.copyToArray(target, 0, 0, data.size)
      assert(target.sameElements("foobar".getBytes()))
    }

    test(s"$name: copyToBuffer") {
      val target = ByteBuffer.wrap(Array.fill[Byte](6)(0))
      data.copyToBuffer(target, 0, data.size)
      assert(target.array.sameElements("foobar".getBytes()))
    }

    test(s"$name: copyToStream") {
      Using.resource(new ByteArrayOutputStream()) { stream =>
        data.copyToStream(stream, 0, data.size)
        assert(stream.toByteArray().sameElements("foobar".getBytes()))
      }
    }
  }

  test("asByteBufferUnsafe") {
    val arr = "Hello, world!".getBytes
    assert(arr eq Blob.view(ByteBuffer.wrap(arr)).asByteBufferUnsafe.array)
    assert(
      arr eq Blob.view(ByteBuffer.wrap(arr)).asByteBufferUnsafe.array
    )
  }

  test("asByteBufferUnsafe has independent position+limit") {
    val bv = Blob.view(ByteBuffer.wrap("Hello, world!".getBytes))
    val bb1 = bv.asByteBufferUnsafe
    assertEquals(bb1.position(), 0)
    assertEquals(bb1.limit(), 13)
    val bb2 = bv.asByteBufferUnsafe
    bb2.position(1)
    bb2.limit(2)
    assertEquals(bb1.position(), 0)
    assertEquals(bb1.limit(), 13)
  }

  test("Array slice: access") {
    val slice = Blob.slice("foobar".getBytes(), 1, 4)
    assert(slice.sameBytesAs(Blob("ooba")))
  }

}
