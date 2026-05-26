/*
 *  Copyright 2021-2026 Disney Streaming
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

import software.amazon.smithy.model.node.Node

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

private[internals] case class PackageConfig(
    prefix: Option[String],
    mappings: Map[String, String],
    allowedNamespaces: Set[NamespacePattern],
    excludedNamespaces: Set[NamespacePattern]
) {

  // explicit mappings take precedence over prefix
  def remap(namespace: String): String =
    mappings.getOrElse(
      namespace,
      prefix.fold(namespace)(p => s"$p.$namespace")
    )
}

private[internals] object PackageConfig {

  val empty: PackageConfig =
    PackageConfig(None, Map.empty, Set.empty, Set.empty)

  def load(metadata: Map[String, Node]): PackageConfig =
    metadata
      .get("smithy4sCodegen")
      .flatMap(_.asObjectNode().toScala)
      .map { obj =>
        val prefix =
          obj.getStringMember("packagePrefix").toScala.map(_.getValue)

        val mappings = obj
          .getObjectMember("packageMappings")
          .toScala
          .map { mapNode =>
            mapNode
              .getMembers()
              .asScala
              .map { case (k, v) =>
                k.getValue -> v.expectStringNode().getValue
              }
              .toMap
          }
          .getOrElse(Map.empty)

        def loadPatterns(field: String): Set[NamespacePattern] =
          obj
            .getArrayMember(field)
            .toScala
            .map { arr =>
              arr
                .getElements()
                .asScala
                .map(n =>
                  NamespacePattern.fromString(n.expectStringNode().getValue)
                )
                .toSet
            }
            .getOrElse(Set.empty)

        PackageConfig(
          prefix,
          mappings,
          loadPatterns("allowedNamespaces"),
          loadPatterns("excludedNamespaces")
        )
      }
      .getOrElse(PackageConfig.empty)
}
