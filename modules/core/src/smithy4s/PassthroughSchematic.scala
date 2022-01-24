/*
 *  Copyright 2021 Disney Streaming
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

import schematic._
import java.util.UUID

class PassthroughSchematic[F[_]](schematic: Schematic[F]) extends Schematic[F] {
  def short: F[Short] = schematic.short

  def int: F[Int] = schematic.int

  def long: F[Long] = schematic.long

  def double: F[Double] = schematic.double

  def float: F[Float] = schematic.float

  def bigint: F[BigInt] = schematic.bigint

  def bigdecimal: F[BigDecimal] = schematic.bigdecimal

  def string: F[String] = schematic.string

  def boolean: F[Boolean] = schematic.boolean

  def uuid: F[UUID] = schematic.uuid

  def byte: F[Byte] = schematic.byte

  def bytes: F[ByteArray] = schematic.bytes

  def unit: F[Unit] = schematic.unit

  def list[S](fs: F[S]): F[List[S]] = schematic.list(fs)

  def set[S](fs: F[S]): F[Set[S]] = schematic.set(fs)

  def vector[S](fs: F[S]): F[Vector[S]] = schematic.vector(fs)

  def map[K, V](fk: F[K], fv: F[V]): F[Map[K, V]] = schematic.map(fk, fv)

  def genericStruct[S](fields: Vector[Field[F, S, _]])(
      const: Vector[Any] => S
  ): F[S] = schematic.genericStruct(fields)(const)

  // format: off
  def struct[S](f: => S): F[S] = schematic.struct(f)
  
  def struct[Z, A0](a0: Field[F,Z,A0])(f: A0 => Z): F[Z] = schematic.struct(a0)(f)
  
  def struct[Z, A0, A1](a0: Field[F,Z,A0], a1: Field[F,Z,A1])(f: (A0, A1) => Z): F[Z] = schematic.struct(a0, a1)(f)
  
  def struct[Z, A0, A1, A2](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2])(f: (A0, A1, A2) => Z): F[Z] = schematic.struct(a0, a1, a2)(f)
  
  def struct[Z, A0, A1, A2, A3](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3])(f: (A0, A1, A2, A3) => Z): F[Z] = schematic.struct(a0, a1, a2, a3)(f)
  
  def struct[Z, A0, A1, A2, A3, A4](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4])(f: (A0, A1, A2, A3, A4) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5])(f: (A0, A1, A2, A3, A4, A5) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6])(f: (A0, A1, A2, A3, A4, A5, A6) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7])(f: (A0, A1, A2, A3, A4, A5, A6, A7) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9], a10: Field[F,Z,A10])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9], a10: Field[F,Z,A10], a11: Field[F,Z,A11])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9], a10: Field[F,Z,A10], a11: Field[F,Z,A11], a12: Field[F,Z,A12])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9], a10: Field[F,Z,A10], a11: Field[F,Z,A11], a12: Field[F,Z,A12], a13: Field[F,Z,A13])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9], a10: Field[F,Z,A10], a11: Field[F,Z,A11], a12: Field[F,Z,A12], a13: Field[F,Z,A13], a14: Field[F,Z,A14])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9], a10: Field[F,Z,A10], a11: Field[F,Z,A11], a12: Field[F,Z,A12], a13: Field[F,Z,A13], a14: Field[F,Z,A14], a15: Field[F,Z,A15])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9], a10: Field[F,Z,A10], a11: Field[F,Z,A11], a12: Field[F,Z,A12], a13: Field[F,Z,A13], a14: Field[F,Z,A14], a15: Field[F,Z,A15], a16: Field[F,Z,A16])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9], a10: Field[F,Z,A10], a11: Field[F,Z,A11], a12: Field[F,Z,A12], a13: Field[F,Z,A13], a14: Field[F,Z,A14], a15: Field[F,Z,A15], a16: Field[F,Z,A16], a17: Field[F,Z,A17])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9], a10: Field[F,Z,A10], a11: Field[F,Z,A11], a12: Field[F,Z,A12], a13: Field[F,Z,A13], a14: Field[F,Z,A14], a15: Field[F,Z,A15], a16: Field[F,Z,A16], a17: Field[F,Z,A17], a18: Field[F,Z,A18])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9], a10: Field[F,Z,A10], a11: Field[F,Z,A11], a12: Field[F,Z,A12], a13: Field[F,Z,A13], a14: Field[F,Z,A14], a15: Field[F,Z,A15], a16: Field[F,Z,A16], a17: Field[F,Z,A17], a18: Field[F,Z,A18], a19: Field[F,Z,A19])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9], a10: Field[F,Z,A10], a11: Field[F,Z,A11], a12: Field[F,Z,A12], a13: Field[F,Z,A13], a14: Field[F,Z,A14], a15: Field[F,Z,A15], a16: Field[F,Z,A16], a17: Field[F,Z,A17], a18: Field[F,Z,A18], a19: Field[F,Z,A19], a20: Field[F,Z,A20])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20)(f)
  
  def struct[Z, A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21](a0: Field[F,Z,A0], a1: Field[F,Z,A1], a2: Field[F,Z,A2], a3: Field[F,Z,A3], a4: Field[F,Z,A4], a5: Field[F,Z,A5], a6: Field[F,Z,A6], a7: Field[F,Z,A7], a8: Field[F,Z,A8], a9: Field[F,Z,A9], a10: Field[F,Z,A10], a11: Field[F,Z,A11], a12: Field[F,Z,A12], a13: Field[F,Z,A13], a14: Field[F,Z,A14], a15: Field[F,Z,A15], a16: Field[F,Z,A16], a17: Field[F,Z,A17], a18: Field[F,Z,A18], a19: Field[F,Z,A19], a20: Field[F,Z,A20], a21: Field[F,Z,A21])(f: (A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21) => Z): F[Z] = schematic.struct(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21)(f)
  // format: on

  def union[S](first: Alt[F, S, _], rest: Vector[Alt[F, S, _]])(
      total: S => Alt.WithValue[F, S, _]
  ): F[S] = schematic.union(first, rest)(total)

  def enumeration[A](
      to: A => (String, Int),
      fromName: Map[String, A],
      fromOrdinal: Map[Int, A]
  ): F[A] = schematic.enumeration(to, fromName, fromOrdinal)

  def suspend[A](f: => F[A]): F[A] = schematic.suspend(f)

  def bijection[A, B](f: F[A], to: A => B, from: B => A): F[B] =
    schematic.bijection(f, to, from)

  def timestamp: F[Timestamp] = schematic.timestamp

  def withHints[A](fa: F[A], hints: Hints): F[A] =
    schematic.withHints(fa, hints)

  def document: F[Document] = schematic.document

}
