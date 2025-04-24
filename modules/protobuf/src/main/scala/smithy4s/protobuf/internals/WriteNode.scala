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

private[internals] object WriteNode {

  val empty: WriteNode = new WriteNode(0) {
    def write(os: CodedOutputStream): Unit = ()
  }

}
