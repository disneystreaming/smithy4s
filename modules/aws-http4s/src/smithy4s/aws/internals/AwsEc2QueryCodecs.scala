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
package aws
package internals

import smithy.api.XmlName
import cats.effect.Concurrent
import cats.syntax.all._
import fs2.compression.Compression
import smithy4s.Endpoint
import smithy4s.http._
import smithy4s.http4s.kernel._
import smithy4s.schema._
import _root_.aws.protocols.Ec2QueryName
import _root_.aws.protocols.AwsQueryError

private[aws] object AwsEcsQueryCodecs {

  def make[F[_]: Concurrent: Compression](
      version: String
  ): UnaryClientCodecs.Make[F] =
    new UnaryClientCodecs.Make[F] {
      // TODO: Caching?
      object foo extends SchemaVisitor.Default[Schema] { self =>
        override def default[A]: Schema[A] = ???
        override def apply[A](schema: Schema[A]): Schema[A] =
          schema.hints.get(Ec2QueryName) match {
            case Some(ec2QueryName) =>
              schema.addHints(XmlName(ec2QueryName.value))

            case None =>
              schema.hints.get(XmlName) match {
                case Some(xmlName) =>
                  schema.addHints(
                    XmlName(xmlName.value.capitalize)
                  )

                case _ =>
                  schema match {
                    case s: Schema.CollectionSchema[_, _] =>
                      s.copy(
                        member = self(s.member)
                      )

                    case m: Schema.MapSchema[_, _] =>
                      m.copy(
                        key = self(m.key),
                        value = self(m.value)
                      )

                    case struct: Schema.StructSchema[_] =>
                      struct.copy(
                        fields = struct.fields.map(field =>
                          field.hints.get(Ec2QueryName) match {
                            case Some(ec2QueryName) =>
                              def transformField[S, B](
                                  field: Field[S, B]
                              ): Field[S, B] =
                                field
                                  .copy(
                                    schema = self(field.schema)
                                  )
                                  .addHints(XmlName(ec2QueryName.value))
                              transformField(field)
                            case None =>
                              def transformField[S, B](
                                  field: Field[S, B]
                              ): Field[S, B] =
                                field.hints.get(XmlName) match {
                                  case Some(xmlName) =>
                                    field
                                      .addHints(
                                        XmlName(xmlName.value.capitalize)
                                      )
                                  case _ =>
                                    println(s"transforming field ${field}")
                                    field                                    
                                      .copy(
                                        schema = self(field.schema)
                                      ).
                                    addHints(
                                      XmlName(field.label.capitalize)
                                    )
                                }
                              transformField(field)
                          }
                        )
                      )
                    case union: Schema.UnionSchema[_] =>
                      union.copy(
                        alternatives = union.alternatives.map(field =>
                          field.addHints(
                            XmlName(field.label.capitalize)
                          )
                        )
                      )
                    case _ => schema
                  }
              }
          }
      }
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, I, E, O] = {
        val requestEncoderCompilers = AwsQueryCodecs
          .requestEncoderCompilers[F](
            ignoreXmlFlattened = true,
            action = endpoint.id.name,
            version = version
          )
          .contramapSchema(
            foo
            // smithy4s.schema.Schema.transformHintsTransitivelyK(hints =>
            //   hints.get(Ec2QueryName) match {
            //     case Some(ec2QueryName) =>
            //       hints ++ smithy4s.Hints(
            //         XmlName(ec2QueryName.value)
            //       )

            //     case None =>
            //       hints.get(XmlName) match {
            //         case Some(xmlName) if !xmlName.value.isEmpty =>
            //           hints ++ smithy4s.Hints(
            //             XmlName(xmlName.value.capitalize)
            //           )

            //         case _ =>
            //           hints
            //           //  ++ smithy4s.Hints(
            //             // XmlName(s"${Character.toUpperCase(xmlName.value.charAt(0))}${xmlName.value.substring(1)}")
            //           // )
            //       }
            //   }
            // )
          )
        val transformEncoders =
          applyCompression[F](endpoint.hints, retainUserEncoding = false)
        val requestEncoderCompilersWithCompression = transformEncoders(
          requestEncoderCompilers
        )

        val responseTag = endpoint.name + "Response"
        val responseDecoderCompilers =
          AwsXmlCodecs
            .responseDecoderCompilers[F]
            .contramapSchema(
              smithy4s.schema.Schema.transformHintsLocallyK(
                _ ++ smithy4s.Hints(
                  smithy4s.xml.internals.XmlStartingPath(
                    List(responseTag)
                  )
                )
              )
            )
        val errorDecoderCompilers = AwsXmlCodecs
          .responseDecoderCompilers[F]
          .contramapSchema(
            smithy4s.schema.Schema.transformHintsLocallyK(
              _ ++ smithy4s.Hints(
                smithy4s.xml.internals.XmlStartingPath(
                  List("Response", "Errors", "Error")
                )
              )
            )
          )

        // Takes the `@awsQueryError` trait into consideration to decide how to
        // discriminate error responses.
        val errorNameMapping: (String => String) = endpoint.errorable match {
          case None =>
            identity[String]

          case Some(err) =>
            val mapping = err.error.alternatives.flatMap { alt =>
              val shapeName = alt.schema.shapeId.name
              alt.hints.get(AwsQueryError).map(_.code).map(_ -> shapeName)
            }.toMap
            (errorCode: String) => mapping.getOrElse(errorCode, errorCode)
        }
        val errorDiscriminator = AwsErrorTypeDecoder
          .fromResponse(errorDecoderCompilers)
          .andThen(_.map(_.map {
            case HttpDiscriminator.NameOnly(name) =>
              HttpDiscriminator.NameOnly(errorNameMapping(name))
            case other => other
          }))

        val make = UnaryClientCodecs.Make[F](
          input = requestEncoderCompilersWithCompression,
          output = responseDecoderCompilers,
          error = errorDecoderCompilers,
          errorDiscriminator = errorDiscriminator
        )
        make.apply(endpoint)
      }
    }

}
