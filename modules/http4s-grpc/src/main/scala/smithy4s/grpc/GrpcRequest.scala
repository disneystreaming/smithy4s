package smithy4s.grpc

import smithy4s.http.HttpUri

case class GrpcRequest[+A](
  uri: HttpUri, //FIXME: figure out if this is needed and how to model it
  headers: Map[CaseInsensitive, Seq[String]], // FIXME: figure out if this is needed and how to model them
  body: A
)
