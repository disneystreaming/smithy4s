/*
 *  Copyright 2021-2022 Disney Streaming
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

class BlobSpec() extends FunSuite {

  test("sameBytesAs works across data structures") {
    assert(Blob("foo").sameBytesAs(Blob("foo".getBytes)))
    assert(Blob("foo").sameBytesAs(Blob(ByteBuffer.wrap("foo".getBytes))))
  }

  test("equals depends on underlying data structure") {
    assert(Blob("foo") != Blob(ByteBuffer.wrap("foo".getBytes)))
  }

  test("ByteBufferBlob.toArray is idempotent, instantiation-wise") {
    val blob = Blob(ByteBuffer.wrap("foo".getBytes))
    assert(blob.toArray != null)
    assert(blob.toArray.eq(blob.toArray))
    assertEquals(Blob(blob.toArray), Blob("foo"))
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

}
