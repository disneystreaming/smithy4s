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

package smithy4s.codegen.internals

private[internals] trait PostProcessor
    extends (CompilationUnit => CompilationUnit) {}

private[internals] object PostProcessor extends PostProcessor {

  val all: List[PostProcessor] = List(PackedInputsShift)

  def apply(unit: CompilationUnit): CompilationUnit = {
    all.foldLeft(unit)((acc, f) => f(acc))
  }

}

private[internals] object PackedInputsShift extends PostProcessor {
  def apply(unit: CompilationUnit): CompilationUnit = {
    val newDecls = unit.declarations.map {
      case s: Service => transformService(s)
      case other      => other
    }
    unit.copy(declarations = newDecls)
  }

  def transformService(s: Service): Service = {
    if (s.hints.contains(Hint.PackedInputs)) {
      val newOps = s.ops.map { op =>
        op.copy(hints = Hint.PackedInputs :: op.hints)
      }
      s.copy(ops = newOps)
    } else s
  }

}
