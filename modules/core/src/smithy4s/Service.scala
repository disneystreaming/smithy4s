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

import kinds._

//format: off
/**
  * Generic representation of a service, as a list of "endpoints" (mapping to smithy operations).
  *
  * This abstraction lets us retrieve all information necessary to the generic implementation of
  * protocols, as well as transform implementations of finally-encoded interfaces into interpreters
  * (polymorphic functions) that operate on initially-encoded GADTs.
  *
  * @tparam Alg: a finally-encoded interface (commonly called algebra) that works
  *   against an abstract "effect" that takes 5 type parameters:
  *   Input, Error, Output, StreamedInput, StreamedOutput
  * @tparam Op: an initially encoded version of the finally-encoded interface. Typically,
  *   a GADT that acts as a reification of the operations. Passing the reified versions
  *   around makes it drastically easier to implement logic generically, without involving
  *   metaprogramming.
  */
trait Service[Alg[_[_, _, _, _, _]]] extends FunctorK5[Alg] with HasId {
  /**
   * A datatype (typically a sealed trait) that reifies an operation call within
   * a service. It essentially captures the input and type indexes that the operation
   * deals with. It also typically captures an input value.
   *
   * It is possible to think of Operation as an "applied [[Endpoint]]",
   * or a "call to an [[Endpoint]]".
   *
   * @tparam I: the input of the operation
   * @tparam E: the error type associated to the operation (typically represented as a sealed-trait)
   * @tparam O: the output of the operation
   * @tparam SI: the streamed input of an operation. Operations can have unary components and streamed components.
   *         For instance, an http call can send headers (unary `I`) and a stream of bytes (streamed `SI`) to the server.
   * @tparam SO: the streamed output of the operation.
   */
  type Operation[I, E, O, SI, SO]

  /**
   * An endpoint is the set of schemas tied to types associated with an [[Operation]].
   * It has a method to wrap the input in an operation instance I => Operation[I, E, O, SI, SO].
   *
   * You can think of the endpoint as a "template for an [[Operation]]". It contains everything
   * needed to decode/encode operation calls to/from low-level representations (like http requests).
   */
  type Endpoint[I, E, O, SI, SO] = smithy4s.Endpoint[Operation, I, E, O, SI, SO]

  /**
   * This is a polymorphic function that runs an instance of an operation and produces an effect F.
   */
  type Interpreter[F[_, _, _, _, _]] = PolyFunction5[Operation, F]

  /**
   * An interpreter specialised for effects of kind `* -> *`, like Try or monofunctor IO.
   */
  type FunctorInterpreter[F[_]] = Interpreter[kinds.Kind1[F]#toKind5]

  /**
   * An interpreter specialised for effects of kind `* -> (*, *)`, like Either or bifunctor IO.
   */
  type BiFunctorInterpreter[F[_, _]] = Interpreter[kinds.Kind2[F]#toKind5]

  /**
   * A polymorphic function that can take an Endpoint (associated to this service) and
   * produces an handler for it, namely a function that takes the input type of the
   * operation, and produces an effect.
   */
  type EndpointCompiler[F[_, _, _, _, _]] = PolyFunction5[Endpoint, Kind5[F]#handler]

  /**
   * A [[EndpointCompiler]] specialised for effects of kind `* -> *`, like Try or monofunctor IO
   */
  type FunctorEndpointCompiler[F[_]] = EndpointCompiler[Kind1[F]#toKind5]

  /**
   * A [[EndpointCompiler]] specialised for effects of kind `* -> (*, *)`, like Either or bifunctor IO
   */
  type BiFunctorEndpointCompiler[F[_, _]] = EndpointCompiler[Kind2[F]#toKind5]

  /**
   * A short-hand for algebras that are specialised for effects of kind `* -> *`.
   *
   * NB: this alias should be used in polymorphic implementations. When using the Smithy4s
   * code generator, equivalent aliases that are named after the service are generated
   * (e.g. `Weather` corresponding to `WeatherGen`).
   */
  type Impl[F[_]] = FunctorAlgebra[Alg, F]

  /**
   * A short-hand for algebras that are specialised for effects of kind `* -> (*, *)`.
   * This is meant to be used in userland, e.g: {{{ val myService = MyService.ErrorAware[Either] }}}
   */
  type ErrorAware[F[_, _]] = BiFunctorAlgebra[Alg, F]

  val service: Service[Alg] = this
  def endpoints: Vector[Endpoint[_, _, _, _, _]]
  def ordinal[I, E, O, SI, SO](op: Operation[I, E, O, SI, SO]) : Int
  def input[I, E, O, SI, SO](op: Operation[I, E, O, SI, SO]): I
  def endpoint[I, E, O, SI, SO](op: Operation[I, E, O, SI, SO]): Endpoint[I, E, O, SI, SO] =
    endpoints(ordinal(op)).asInstanceOf[Endpoint[I, E, O, SI, SO]]
  def version: String
  def hints: Hints
  def reified: Alg[Operation]
  def fromPolyFunction[P[_, _, _, _, _]](function: PolyFunction5[Operation, P]): Alg[P]
  def toPolyFunction[P[_, _, _, _, _]](algebra: Alg[P]): PolyFunction5[Operation, P]

  final val opToEndpoint: PolyFunction5[Operation, Endpoint] = new PolyFunction5[Operation, Endpoint]{
    def apply[I, E, O, SI, SO](op: Operation[I,E,O,SI,SO]): Endpoint[I,E,O,SI,SO] =
      endpoint(op)
  }

  /**
   * Given a generic way to turn an endpoint into some handling function (like `I => F[I, E, O, SI, SO]`), this method
   * takes care of the logic necessary to produce an interpreter that takes an Operation associated
   * to the service and routes it to the correct function, returning the result.
   */
  final def interpreter[F[_, _, _, _, _]](compiler: EndpointCompiler[F]) : Interpreter[F] = new Interpreter[F]{
    private val cache: Array[Any] = {
      val builder = scala.collection.mutable.ArrayBuffer[Any]()
      endpoints.foreach(ep =>
        builder += compiler(ep).asInstanceOf[Any]
      )
      builder.toArray
    }
    def apply[I, E, O, SI, SO](operation: Operation[I, E, O, SI, SO]): F[I, E, O, SI, SO] = {
      cache(ordinal(operation)).asInstanceOf[I => F[I, E, O, SI, SO]].apply(input(operation))
    }
  }

  /**
   * A monofunctor-specialised version of [[interpreter]]
   */
  final def functorInterpreter[F[_]](compiler: FunctorEndpointCompiler[F]): FunctorInterpreter[F] = interpreter[Kind1[F]#toKind5](compiler)

  /**
   * A bifunctor-specialised version of [[interpreter]]
   */
  final def bifunctorInterpreter[F[_, _]](compiler: BiFunctorEndpointCompiler[F]): BiFunctorInterpreter[F] = interpreter[Kind2[F]#toKind5](compiler)


  /**
   * A function that takes an endpoint compiler and produces an Algebra (typically an instance of the generated interfaces),
   * backed by an interpreter.
   *
   * This is useful for writing generic functions that result in the instantiation of a client instance that abides by
   * the service interface.
   */
  final def algebra[F[_, _, _, _, _]](compiler: EndpointCompiler[F]) : Alg[F] = fromPolyFunction(interpreter(compiler))

  /**
   * A monofunctor-specialised version of [[algebra]]
   */
  final def impl[F[_]](compiler: FunctorEndpointCompiler[F]) : Impl[F] = algebra[Kind1[F]#toKind5](compiler)

  /**
   * A monofunctor-specialised version of [[algebra]]
   */
  final def errorAware[F[_, _]](compiler: BiFunctorEndpointCompiler[F]) : ErrorAware[F] = algebra[Kind2[F]#toKind5](compiler)

}

object Service {

  def apply[Alg[_[_, _, _, _, _]]](implicit ev: Service[Alg]): ev.type = ev

  type Aux[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]] = Service[Alg]{ type Operation[I, E, O, SI, SO] = Op[I, E, O, SI, SO] }

  trait Mixin[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]] extends Service[Alg]{
    implicit val serviceInstance: Service[Alg] = this
    type Operation[I, E, O, SI, SO] = Op[I, E, O, SI, SO]
  }

  /**
    * A Service the algebra of which is a PolyFunction.
    */
  trait Reflective[Op[_, _, _, _, _]] extends Service[PolyFunction5.From[Op]#Algebra] {
    type Operation[I, E, O, SI, SO] = Op[I, E, O, SI, SO]
    final def reified: PolyFunction5[Op, Op] = PolyFunction5.identity
    final def fromPolyFunction[P[_, _, _, _, _]](function: PolyFunction5[Op, P]): PolyFunction5[Op, P] = function
    final def toPolyFunction[P[_, _, _, _, _]](algebra: PolyFunction5[Op, P]): PolyFunction5[Op, P] = algebra
    final def mapK5[F[_, _, _, _, _], G[_, _, _, _, _]](algebra: PolyFunction5[Op, F], function: PolyFunction5[F, G]): PolyFunction5[Op, G] = algebra.andThen(function)
  }
}
