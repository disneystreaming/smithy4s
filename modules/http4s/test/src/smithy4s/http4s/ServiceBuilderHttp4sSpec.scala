// /*
//  *  Copyright 2021-2022 Disney Streaming
//  *
//  *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
//  *  you may not use this file except in compliance with the License.
//  *  You may obtain a copy of the License at
//  *
//  *     https://disneystreaming.github.io/TOST-1.0.txt
//  *
//  *  Unless required by applicable law or agreed to in writing, software
//  *  distributed under the License is distributed on an "AS IS" BASIS,
//  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  *  See the License for the specific language governing permissions and
//  *  limitations under the License.
//  */

// package smithy4s.http4s

// import cats.effect.IO
// import org.http4s.implicits._
// import org.http4s.Request
// import smithy4s.example.guides.auth.{
//   World,
//   HealthCheckOutput,
//   HelloWorldAuthServiceGen,
//   HelloWorldAuthService
// }
// import smithy4s.example.{
//   UnknownServerError,
//   UnknownServerErrorCode,
//   HealthResponse,
//   PizzaAdminService,
//   PizzaAdminServiceGen
// }
// import smithy4s.kinds.PolyFunction5
// import smithy4s.{Endpoint, Hints, Service}
// import weaver.SimpleIOSuite

// object ServiceBuilderHttp4sSpec extends SimpleIOSuite {

//   test("Capable of altering the URI path of an endpoint") {
//     val serviceImpl: HelloWorldAuthService[IO] = new HelloWorldAuthService[IO] {
//       override def sayWorld(): IO[World] = IO.pure(World("hello"))

//       override def healthCheck(): IO[HealthCheckOutput] = ???
//     }

//     val builder = Service.Builder.fromService(HelloWorldAuthService)

//     val mapper = new PolyFunction5[
//       HelloWorldAuthServiceGen.Endpoint,
//       HelloWorldAuthServiceGen.Endpoint
//     ] {
//       def apply[I, E, O, SI, SO](
//           op: HelloWorldAuthServiceGen.Endpoint[I, E, O, SI, SO]
//       ): HelloWorldAuthServiceGen.Endpoint[I, E, O, SI, SO] = {
//         if (op.name == "SayWorld") {
//           Endpoint.Builder
//             .fromEndpoint(op)
//             .withHints(
//               Hints(
//                 smithy.api.Http(
//                   method = smithy.api.NonEmptyString("GET"),
//                   uri = smithy.api.NonEmptyString("/yeap"),
//                   code = 200
//                 ),
//                 smithy.api.Readonly()
//               )
//             )
//             .build
//         } else {
//           op
//         }
//       }
//     }
//     val modifiedService = builder
//       .mapEndpointEach(mapper)
//       .build

//     SimpleRestJsonBuilder(modifiedService)
//       .routes(serviceImpl)
//       .resource
//       .use { routes =>
//         routes.orNotFound.run(Request[IO](uri = uri"/yeap")).map { response =>
//           assert(response.status.code == 200)
//         }
//       }
//   }

//   test(
//     "when an errorable is removed and the service raises an error, it behaves in the same way as any other throwable"
//   ) {
//     val serviceImpl: PizzaAdminService[IO] =
//       new PizzaAdminService.Default[IO](IO.stub) {
//         override def health(query: Option[String]): IO[HealthResponse] =
//           IO.raiseError(
//             UnknownServerError(
//               UnknownServerErrorCode.ERROR_CODE
//             )
//           )

//       }

//     val servicebuilder = Service.Builder.fromService(PizzaAdminService)
//     val mapper = new PolyFunction5[
//       PizzaAdminServiceGen.Endpoint,
//       PizzaAdminServiceGen.Endpoint
//     ] {
//       def apply[I, E, O, SI, SO](
//           op: PizzaAdminServiceGen.Endpoint[I, E, O, SI, SO]
//       ): PizzaAdminServiceGen.Endpoint[I, E, O, SI, SO] =
//         Endpoint.Builder
//           .fromEndpoint(op)
//           .mapErrorable(_ => None)
//           .build
//     }

//     val modifiedService = servicebuilder
//       .mapEndpointEach(mapper)
//       .build

//     SimpleRestJsonBuilder(modifiedService)
//       .routes(serviceImpl)
//       .resource
//       .use { routes =>
//         routes.orNotFound.run(Request[IO](uri = uri"/health")).attempt.map {
//           response =>
//             assert(
//               response == Left(
//                 UnknownServerError(UnknownServerErrorCode.ERROR_CODE)
//               )
//             )
//         }
//       }
//   }
// }
