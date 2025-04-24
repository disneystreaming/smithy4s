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
