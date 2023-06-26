---
sidebar_label: Service Product
title: Service Product
---

As of smithy4s version `0.18.x` you can also generate a service interface in
which each method doesn't receive an input. Instead, the output of each method
has the usual return type, which already includes the input as a type parameter.
We call this version a "service product" because it can be seen as the product
of all the operations of the service.

To generate a service product, annotate the service definition with

`@generateServiceProduct`:

```kotlin
@SERVICE_PRODUCT_SPEC@
```

This will generate the following interface:

```scala
trait ExampleServiceProductGen[F[_, _, _, _, _]] {
  def exampleOperation: F[ExampleInput, ExampleServiceOperation.ExampleOperationError, ExampleOutput, Nothing, Nothing]
}
```

and the following implementation of `ServiceProduct`:

```scala
object ExampleServiceProductGen extends ServiceProduct[ExampleServiceProductGen]
```

You will be able to access the service product version of the service like this:

```scala mdoc
import smithy4s.example.product._

ExampleServiceGen.serviceProduct
```

Or, more generically:

```scala mdoc
import smithy4s.ServiceProduct

def productOf[Alg[_[_, _, _, _, _]]](mirror: ServiceProduct.Mirror[Alg]) = mirror.serviceProduct

// example
def exampleProduct = productOf(ExampleService)
```

With service products, you can call service methods without providing their inputs directly.

Here are a couple ways you can use this as a library author:

### Static description of services

```scala mdoc
import smithy4s.kinds.PolyFunction5

type Describe[_, _, _, _, _] = String

def descriptor[Alg[_[_, _, _, _, _]]](mirror: ServiceProduct.Mirror[Alg]): mirror.Prod[Describe] =
  mirror
    .serviceProduct
    .mapK5(
      mirror.serviceProduct.endpointsProduct,
      new PolyFunction5[mirror.serviceProduct.service.Endpoint, Describe] {

        override def apply[I, E, O, SI, SO](
          fa: mirror.serviceProduct.service.Endpoint[I, E, O, SI, SO]
        ): Describe[I, E, O, SI, SO] =
          s"def ${fa.name}(input: ${fa.input.shapeId.name}): ${fa.output.shapeId.name}"

      },
    )

// Usage

val desc: String = descriptor(ExampleService).exampleOperation
```

### Non-linear input of operation

```scala mdoc
import smithy4s.ShapeId

type Id[A] = A

val impl: ExampleService[Id] =
  new ExampleService[Id] {

    override def exampleOperation(input: String): ExampleOutput = ExampleOutput(
      s"Output for $input!"
    )

  }

type ListClient[I, _, O, _, _] = List[I] => List[O]

def listClient[Alg[_[_, _, _, _, _]], Prod[_[_, _, _, _, _]]](
  impl: smithy4s.kinds.FunctorAlgebra[Alg, Id]
)(
  implicit sp: ServiceProduct.Aux[Prod, Alg]
): Prod[ListClient] = sp
  .mapK5(
    sp.endpointsProduct,
    new PolyFunction5[sp.service.Endpoint, ListClient] {

      private val interp = sp.service.toPolyFunction(impl)

      override def apply[I, E, O, SI, SO](
        fa: sp.service.Endpoint[I, E, O, SI, SO]
      ): List[I] => List[O] = _.map(in => interp(fa.wrap(in)))

    },
  )

// Usage

listClient(impl)( /* implicit scope problem here - TODO */ ExampleService.serviceProduct)
  .exampleOperation(
    List("a", "b", "c").map(ExampleInput(_))
  )
  .mkString("\n")
```

### Fluent service builder

```scala mdoc

type ToList[_, _, O, _, _] = List[O]

trait EndpointHandlerBuilder[I, E, O, SI, SO] {
  def apply(f: I => O): EndpointHandler
}

sealed trait EndpointHandler {
  type I_
  type O_
  def id: ShapeId
  def function: I_ => O_
}

case class PartialBuilder[Alg[_[_, _, _, _, _]], Prod[_[_, _, _, _, _]]](
  mirror: ServiceProduct.Mirror.Aux[Alg, Prod],
  handlers: List[EndpointHandler],
) {
  private val sp: ServiceProduct.Aux[Prod, Alg] = mirror.serviceProduct

  private val ehbProduct = sp
    .mapK5(
      sp.endpointsProduct,
      new PolyFunction5[sp.service.Endpoint, EndpointHandlerBuilder] {

        override def apply[I, E, O, SI, SO](
          fa: sp.service.Endpoint[I, E, O, SI, SO]
        ): EndpointHandlerBuilder[I, E, O, SI, SO] =
          new EndpointHandlerBuilder[I, E, O, SI, SO] {

            override def apply(f: I => O): EndpointHandler =
              new EndpointHandler {
                type I_ = I
                type O_ = O
                override val id: ShapeId = fa.id
                override val function: I_ => O_ = f
              }

          }

      },
    )

  def build: Alg[ToList] = sp
    .service
    .algebra(new sp.service.EndpointCompiler[ToList] {

      override def apply[I, E, O, SI, SO](
        fa: sp.service.Endpoint[I, E, O, SI, SO]
      ): I => List[O] = {

        val matchingHandlers = handlers
          .filter(_.id == fa.id)
          // A bit of type unsafety, to simplify things
          .map(_.function.asInstanceOf[I => O])

        i => matchingHandlers.map(_.apply(i))

      }

    })

  def withHandler(
    op: Prod[EndpointHandlerBuilder] => EndpointHandler
  ): PartialBuilder[Alg, Prod] = copy(handlers = handlers :+ op(ehbProduct))

}

def partialBuilder[Alg[_[_, _, _, _, _]]](
  mirror: ServiceProduct.Mirror[Alg]
): PartialBuilder[Alg, mirror.Prod] = new PartialBuilder[Alg, mirror.Prod](mirror, handlers = Nil)

// Usage

val listService: ExampleServiceGen[ToList] =
  partialBuilder(ExampleService)
    .withHandler(_.exampleOperation { (in: ExampleInput) =>
      ExampleOutput(s"First output for ${in.a}!")
    })
    .withHandler(_.exampleOperation { (in: ExampleInput) =>
      ExampleOutput(s"Another output for ${in.a}!")
    })
    .build

listService.exampleOperation("hello").mkString("\n")
```
