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

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.Trait
import software.amazon.smithy.openapi.OpenApiConfig
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter
import software.amazon.smithy.openapi.fromsmithy.Smithy2OpenApiExtension

import java.util.ServiceLoader
import scala.jdk.CollectionConverters._

package object openapi {

  /**
    * Creates open-api representations for all services in a model
    * that are annotated with the `smithy4s.api#simpleRestJson`
    * trait.
    */
  def convert(
      model: Model,
      allowedNS: Option[Set[String]],
      classLoader: ClassLoader
  ): List[OpenApiConversionResult] = {
    val services = model
      .shapes()
      .iterator()
      .asScala
      .collect {
        case s if s.isServiceShape() => s.asServiceShape().get()
      }
      .toList

    val filteredServices: List[ServiceShape] = allowedNS match {
      case Some(allowed) =>
        services.filter(s => allowed(s.getId().getNamespace()))
      case None => services
    }

    import scala.jdk.CollectionConverters._

    case class TraitKey[T <: Trait](cls: Class[T]) {
      def getIdIfApplied(shape: Shape): Option[ShapeId] = {
        val maybeTrait = shape.getTrait(cls)
        if (maybeTrait.isPresent()) {
          Some(maybeTrait.get().toShapeId())
        } else None
      }
    }
    val openapiAwareTraits: Set[TraitKey[_]] = ServiceLoader
      .load(
        classOf[Smithy2OpenApiExtension],
        classLoader
      )
      .iterator()
      .asScala
      .toVector
      .flatMap(_.getProtocols().asScala.map(p => TraitKey(p.getProtocolType())))
      .toSet

    filteredServices.flatMap { service =>
      val protocols: Set[ShapeId] =
        openapiAwareTraits.flatMap(_.getIdIfApplied(service))
      protocols.map { protocol =>
        val serviceId = service.getId()
        val config = new OpenApiConfig()
        config.setService(serviceId)
        config.setProtocol(protocol)
        val openapi = OpenApiConverter.create().config(config).convert(model)
        val jsonString = Node.prettyPrintJson(openapi.toNode())
        OpenApiConversionResult(protocol, serviceId, jsonString)
      }
    }
  }

  def convert(
      model: Model,
      allowedNS: Option[Set[String]]
  ): List[OpenApiConversionResult] =
    convert(model, allowedNS, this.getClass().getClassLoader())

  implicit class OptionalExt[A](opt: java.util.Optional[A]) {
    def asScala: Option[A] = if (opt.isPresent()) Some(opt.get()) else None
  }

}
