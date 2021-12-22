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

package schematic
package syntax

object stdlib
    extends CommonSyntax
    with list.ClosedSyntax[Schematic.stdlib]
    with vector.ClosedSyntax[Schematic.stdlib]
    with set.ClosedSyntax[Schematic.stdlib]
    with map.ClosedSyntax[Schematic.stdlib]
    with union.ClosedSyntax[Schematic.stdlib]
    with struct.ClosedSyntax[Schematic.stdlib]
    with suspended.ClosedSyntax[Schematic.stdlib]
    with bijection.ClosedSyntax[Schematic.stdlib]
