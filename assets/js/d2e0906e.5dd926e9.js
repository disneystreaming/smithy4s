"use strict";(self.webpackChunksmithy4s=self.webpackChunksmithy4s||[]).push([[2419],{3905:(e,t,n)=>{n.d(t,{Zo:()=>c,kt:()=>u});var a=n(7294);function i(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function r(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);t&&(a=a.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,a)}return n}function o(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?r(Object(n),!0).forEach((function(t){i(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):r(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function s(e,t){if(null==e)return{};var n,a,i=function(e,t){if(null==e)return{};var n,a,i={},r=Object.keys(e);for(a=0;a<r.length;a++)n=r[a],t.indexOf(n)>=0||(i[n]=e[n]);return i}(e,t);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);for(a=0;a<r.length;a++)n=r[a],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(i[n]=e[n])}return i}var l=a.createContext({}),p=function(e){var t=a.useContext(l),n=t;return e&&(n="function"==typeof e?e(t):o(o({},t),e)),n},c=function(e){var t=p(e.components);return a.createElement(l.Provider,{value:t},e.children)},m="mdxType",d={inlineCode:"code",wrapper:function(e){var t=e.children;return a.createElement(a.Fragment,{},t)}},h=a.forwardRef((function(e,t){var n=e.components,i=e.mdxType,r=e.originalType,l=e.parentName,c=s(e,["components","mdxType","originalType","parentName"]),m=p(n),h=i,u=m["".concat(l,".").concat(h)]||m[h]||d[h]||r;return n?a.createElement(u,o(o({ref:t},c),{},{components:n})):a.createElement(u,o({ref:t},c))}));function u(e,t){var n=arguments,i=t&&t.mdxType;if("string"==typeof e||i){var r=n.length,o=new Array(r);o[0]=h;var s={};for(var l in t)hasOwnProperty.call(t,l)&&(s[l]=t[l]);s.originalType=e,s[m]="string"==typeof e?e:i,o[1]=s;for(var p=2;p<r;p++)o[p]=n[p];return a.createElement.apply(null,o)}return a.createElement.apply(null,n)}h.displayName="MDXCreateElement"},6701:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>l,contentTitle:()=>o,default:()=>d,frontMatter:()=>r,metadata:()=>s,toc:()=>p});var a=n(7462),i=(n(7294),n(3905));const r={sidebar_label:"Dynamic module",title:"Dynamic module"},o=void 0,s={unversionedId:"guides/dynamic",id:"guides/dynamic",title:"Dynamic module",description:"Introduction",source:"@site/../docs/target/jvm-2.13/mdoc/06-guides/dynamic.md",sourceDirName:"06-guides",slug:"/guides/dynamic",permalink:"/smithy4s/docs/guides/dynamic",draft:!1,editUrl:"https://github.com/disneystreaming/smithy4s/edit/main/modules/docs/src/06-guides/dynamic.md",tags:[],version:"current",frontMatter:{sidebar_label:"Dynamic module",title:"Dynamic module"},sidebar:"tutorialSidebar",previous:{title:"Services and endpoints",permalink:"/smithy4s/docs/design/services"},next:{title:"Endpoint Specific Middleware",permalink:"/smithy4s/docs/guides/endpoint-middleware"}},l={},p=[{value:"Introduction",id:"introduction",level:2},{value:"(Why) do we need codegen?",id:"why-do-we-need-codegen",level:2},{value:"The Dynamic way",id:"the-dynamic-way",level:2},{value:"Loading a dynamic model",id:"loading-a-dynamic-model",level:2},{value:"Using the DSI",id:"using-the-dsi",level:2},{value:"Case study: dynamic HTTP client",id:"case-study-dynamic-http-client",level:2}],c={toc:p},m="wrapper";function d(e){let{components:t,...n}=e;return(0,i.kt)(m,(0,a.Z)({},c,n,{components:t,mdxType:"MDXLayout"}),(0,i.kt)("h2",{id:"introduction"},"Introduction"),(0,i.kt)("p",null,"It is highly recommended to learn about ",(0,i.kt)("a",{parentName:"p",href:"/smithy4s/docs/design/design"},"the library's design")," before going into this section."),(0,i.kt)("p",null,"Smithy4s is first and foremost a code generation tool for the Smithy language in Scala. Although it does provide interpreters for the Smithy services, which can be used to derive e.g. HTTP clients and servers, the codegen way can only get you so far - there are some situations when it's ",(0,i.kt)("strong",{parentName:"p"},"not sufficient for the job"),"."),(0,i.kt)("p",null,"Code generation works well if your Smithy model changes ",(0,i.kt)("strong",{parentName:"p"},"no more often")," than your service's implementation - as long as you run your build whenever you make a code change, codegen will also be triggered,\nand the Scala compiler will ensure you're in sync with the Smithy model in its present state. But what if your Smithy model changes are ",(0,i.kt)("strong",{parentName:"p"},"more frequent")," than the service? Or what if you simply don't have access to all the Smithy models your code might have to work with?"),(0,i.kt)("p",null,"These cases, and possibly others, are why Smithy4s has the ",(0,i.kt)("inlineCode",{parentName:"p"},"dynamic")," module."),(0,i.kt)("h2",{id:"why-do-we-need-codegen"},"(Why) do we need codegen?"),(0,i.kt)("p",null,"As you know by now, Smithy4s's codegen is static - it requires the model to be available at build-time, so that code can be generated and made available to you at compile-time."),(0,i.kt)("p",null,"In short, what happens at ",(0,i.kt)("strong",{parentName:"p"},"build-time")," are the following steps:"),(0,i.kt)("ol",null,(0,i.kt)("li",{parentName:"ol"},(0,i.kt)("strong",{parentName:"li"},"Read")," the Smithy files available to your build"),(0,i.kt)("li",{parentName:"ol"},(0,i.kt)("strong",{parentName:"li"},"Build")," a ",(0,i.kt)("a",{parentName:"li",href:"https://smithy.io/2.0/spec/model.html"},"semantic Smithy model"),", which is roughly a graph of shapes that refer to each other"),(0,i.kt)("li",{parentName:"ol"},(0,i.kt)("strong",{parentName:"li"},"Generate")," files for each relevant shape in the model (e.g. a service, a structure, an enum...), including metadata (",(0,i.kt)("a",{parentName:"li",href:"/smithy4s/docs/design/services"},"services")," and ",(0,i.kt)("a",{parentName:"li",href:"/smithy4s/docs/design/schemas"},"schemas"),").")),(0,i.kt)("p",null,"Then, there's the ",(0,i.kt)("strong",{parentName:"p"},"runtime")," part. Let's say you're building an HTTP client - in that case, what you see as a Smithy4s user is:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},"SimpleRestJson(WeatherService)\n  .client(??? : org.http4s.client.Client[IO])\n  .make\n")),(0,i.kt)("p",null,"or more generically:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre"},"interpretToRestClient(WeatherService)\n")),(0,i.kt)("p",null,"The steps that the HTTP client interpreter performs to build a high-level client are:"),(0,i.kt)("ol",null,(0,i.kt)("li",{parentName:"ol"},(0,i.kt)("strong",{parentName:"li"},"Capture")," a ",(0,i.kt)("a",{parentName:"li",href:"/smithy4s/docs/design/services"},"Smithy4s service")," representing the service you wrote in Smithy. This was generated by Smithy4s's codegen."),(0,i.kt)("li",{parentName:"ol"},(0,i.kt)("strong",{parentName:"li"},"Analyze")," the service's endpoints, their input/output schemas, the Hints on these schemas..."),(0,i.kt)("li",{parentName:"ol"},(0,i.kt)("strong",{parentName:"li"},"Transform")," the service description into a high-level proxy to the underlying client implementation.")),(0,i.kt)("p",null,"Turns out that interpreters like this ",(0,i.kt)("strong",{parentName:"p"},"aren't ",(0,i.kt)("em",{parentName:"strong"},"actually")," aware")," of the fact that there's code generation involved. As long as you can provide a data structure describing your service, its endpoints and their schemas (which is indeed the ",(0,i.kt)("inlineCode",{parentName:"p"},"Service")," type),\nyou can use any interpreter that requires one: code generation is just ",(0,i.kt)("strong",{parentName:"p"},"a means to derive")," such a data structure automatically from your Smithy model."),(0,i.kt)("p",null,"This all is why ",(0,i.kt)("strong",{parentName:"p"},"you don't ",(0,i.kt)("em",{parentName:"strong"},"need")," code generation")," to benefit from the interpreters - you just need a way to instantiate a Smithy4s Service (or Schema, if that's what your interpreter operates on)."),(0,i.kt)("p",null,"The Dynamic module of smithy4s was made exactly for that purpose."),(0,i.kt)("h2",{id:"the-dynamic-way"},"The Dynamic way"),(0,i.kt)("p",null,"In the previous section, we looked at the steps performed at build time to generate code:"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},"Read Smithy files"),(0,i.kt)("li",{parentName:"ul"},"Build a Smithy model"),(0,i.kt)("li",{parentName:"ul"},"Generate Scala files with Smithy4s schemas.")),(0,i.kt)("p",null,"Don't be fooled - although we had Smithy files as the input and Scala files as the output, the really important part was getting from the Smithy model to the ",(0,i.kt)("strong",{parentName:"p"},"Service and Schema instances")," representing it.\nThe Dynamic module of smithy4s provides a way to ",(0,i.kt)("strong",{parentName:"p"},"do this at runtime"),"."),(0,i.kt)("p",null,"And the runtime part, where the interpreter runs? ",(0,i.kt)("strong",{parentName:"p"},"It's the same as before!")," The Service and Schema interfaces are identical regardless of the static/dynamic usecase, and so are the interpreters",(0,i.kt)("sup",{parentName:"p",id:"fnref-1"},(0,i.kt)("a",{parentName:"sup",href:"#fn-1",className:"footnote-ref"},"1")),"."),(0,i.kt)("h2",{id:"loading-a-dynamic-model"},"Loading a dynamic model"),(0,i.kt)("p",null,"First of all, you need the dependency:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'libraryDependencies ++= Seq(\n  // version sourced from the plugin\n  "com.disneystreaming.smithy4s"  %% "smithy4s-dynamic" % smithy4sVersion.value\n)\n')),(0,i.kt)("p",null,"Now, you need a Smithy model. There are essentially three ways to get one:"),(0,i.kt)("ol",null,(0,i.kt)("li",{parentName:"ol"},"Load a model using the ",(0,i.kt)("a",{parentName:"li",href:"https://github.com/awslabs/smithy"},"awslabs/smithy")," library's ",(0,i.kt)("inlineCode",{parentName:"li"},"ModelAssembler")),(0,i.kt)("li",{parentName:"ol"},"Load a serialized model from a JSON file (",(0,i.kt)("a",{parentName:"li",href:"https://github.com/disneystreaming/smithy4s/blob/4e678c5f89599f962dc18fb7dcdf3d5d6c0a402b/sampleSpecs/lambda.json"},"example"),"), or"),(0,i.kt)("li",{parentName:"ol"},"Deserialize or generate the ",(0,i.kt)("inlineCode",{parentName:"li"},"smithy4s.dynamic.model.Model")," data structure in any way you want, on your own.")),(0,i.kt)("p",null,"The ",(0,i.kt)("inlineCode",{parentName:"p"},"ModelAssembler")," way only works on the JVM, because Smithy's reference implementation is a Java library. We'll use that way for this guide:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'import software.amazon.smithy.model.Model\n\nval s = """\n$version: "2"\n\nnamespace weather\n\nuse alloy#simpleRestJson\n\n@simpleRestJson\nservice WeatherService {\n    operations: [GetWeather]\n}\n\n@http(method: "GET", uri: "/weather/{city}")\noperation GetWeather {\n    input := {\n        @httpLabel\n        @required\n        city: String\n    }\n    output := {\n        @required\n        weather: String\n    }\n}\n\nstructure Dog {\n    @required\n    name: String\n}\n"""\n\nval model = Model\n  .assembler()\n  .addUnparsedModel(\n    "weather.smithy",\n    s,\n  )\n  .discoverModels()\n  .assemble()\n  .unwrap()\n')),(0,i.kt)("p",null,"The entrypoint to loading models is ",(0,i.kt)("inlineCode",{parentName:"p"},"DynamicSchemaIndex"),"."),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},"import smithy4s.dynamic.DynamicSchemaIndex\nval dsi = DynamicSchemaIndex.loadModel(model).toTry.get\n// dsi: DynamicSchemaIndex = smithy4s.dynamic.internals.DynamicSchemaIndexImpl@5fb70192\n")),(0,i.kt)("p",null,"For alternative ways to load a DSI, see ",(0,i.kt)("inlineCode",{parentName:"p"},"DynamicSchemaIndex.load"),"."),(0,i.kt)("h2",{id:"using-the-dsi"},"Using the DSI"),(0,i.kt)("p",null,"Having a ",(0,i.kt)("inlineCode",{parentName:"p"},"DynamicSchemaIndex"),", we can iterate over all the services available to it:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'dsi.allServices.map(_.service.id)\n// res0: List[smithy4s.ShapeId] = List(\n//   ShapeId(namespace = "weather", name = "WeatherService")\n// )\n')),(0,i.kt)("p",null,"as well as the schemas:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'dsi.allSchemas.map(_.shapeId).filter(_.namespace == "weather")\n// res1: Vector[smithy4s.ShapeId] = Vector(\n//   ShapeId(namespace = "weather", name = "GetWeatherInput"),\n//   ShapeId(namespace = "weather", name = "GetWeatherOutput"),\n//   ShapeId(namespace = "weather", name = "Dog")\n// )\n')),(0,i.kt)("p",null,"You can also access a service or schema by ID:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'import smithy4s.ShapeId\n\ndsi.getService(ShapeId("weather", "WeatherService")).get.service.id\n// res2: ShapeId = ShapeId(namespace = "weather", name = "WeatherService")\ndsi.getSchema(ShapeId("weather", "Dog")).get.shapeId\n// res3: ShapeId = ShapeId(namespace = "weather", name = "Dog")\n')),(0,i.kt)("p",null,"Note that you don't know the exact type of a schema at compile-time:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'import smithy4s.Schema\n\ndsi.getSchema(ShapeId("weather", "Dog")).get\n// res4: Schema[_] = StructSchema(\n//   shapeId = ShapeId(namespace = "weather", name = "Dog"),\n//   hints = Hints(),\n//   fields = Vector(\n//     Required(\n//       label = "name",\n//       instance = PrimitiveSchema(\n//         shapeId = ShapeId(namespace = "smithy.api", name = "String"),\n//         hints = Hints({smithy.api#required={}}),\n//         tag = PString\n//       ),\n//       get = Accessor(index = 0)\n//     )\n//   ),\n//   make = <function1>\n// )\n')),(0,i.kt)("p",null,"It is very similar for services. This is simply due to the fact that at compile-time (which is where typechecking happens) we have no clue what the possible type of the schema could be.\nAfter all, the String representing the model doesn't have to be constant - it could be fetched from the network, and even vary throughout the lifetime of our application!"),(0,i.kt)("p",null,"This doesn't forbid us from using these dynamic goodies in interpreters, though."),(0,i.kt)("h2",{id:"case-study-dynamic-http-client"},"Case study: dynamic HTTP client"),(0,i.kt)("p",null,"Let's make a REST client for a dynamic service. We'll start by writing an interpreter."),(0,i.kt)("p",null,(0,i.kt)("em",{parentName:"p"},'"But wait, weren\'t we supposed to be able to use the existing interpreters?"')),(0,i.kt)("p",null,"That's true - the underlying implementation will use the interpreters made for the static world as well.\nHowever, due to the strangely-typed nature of the dynamic world, we have to deal with some complexity that's normally invisible to the user."),(0,i.kt)("p",null,"For example, you can write this in the static world:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'import cats.effect.IO\nimport weather._\n\n// imagine this comes from an interpreter\nval client: WeatherService[IO] = ??? : WeatherService[IO]\n\nclient.getWeather(city = "hello")\n')),(0,i.kt)("p",null,"but you can't do it if your model gets loaded dynamically! You wouldn't be able to compile that code, because there's no way to tell what services you'll load at runtime."),(0,i.kt)("p",null,"This means that we'll need a different way to pass the following pieces to an interpreter at runtime:"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},"the service being used (",(0,i.kt)("inlineCode",{parentName:"li"},"HelloWorldService"),")"),(0,i.kt)("li",{parentName:"ul"},"the operation being called (",(0,i.kt)("inlineCode",{parentName:"li"},"Hello"),")"),(0,i.kt)("li",{parentName:"ul"},"the operation input (a single parameter: ",(0,i.kt)("inlineCode",{parentName:"li"},"name")," = ",(0,i.kt)("inlineCode",{parentName:"li"},'"Hello"'),")")),(0,i.kt)("p",null,"Let's get to work - we'll need a function that takes a service and its interpreter, the operation name, and some representation of its input. For that input, we'll use smithy4s's ",(0,i.kt)("inlineCode",{parentName:"p"},"Document")," type."),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},"import smithy4s.Document\nimport smithy4s.Endpoint\nimport smithy4s.Service\nimport smithy4s.kinds.FunctorAlgebra\nimport smithy4s.kinds.FunctorInterpreter\nimport cats.effect.IO\n\ndef run[Alg[_[_, _, _, _, _]]](\n  service: Service[Alg],\n  operationName: String,\n  input: Document,\n  alg: FunctorAlgebra[Alg, IO]\n): IO[Document] = {\n  val endpoint = service.endpoints.find(_.id.name == operationName).get\n\n  runEndpoint(endpoint, input, service.toPolyFunction(alg))\n}\n\ndef runEndpoint[Op[_, _, _, _, _], I, O](\n  endpoint: Endpoint[Op, I, _, O, _, _],\n  input: Document,\n  interp: FunctorInterpreter[Op, IO],\n): IO[Document] = {\n  // Deriving these codecs is a costly operation, so we don't recommend doing it for every call.\n  // We do it here for simplicity.\n  val inputDecoder = Document.Decoder.fromSchema(endpoint.input)\n  val outputEncoder = Document.Encoder.fromSchema(endpoint.output)\n\n  val decoded: I = inputDecoder.decode(input).toTry.get\n\n  val result: IO[O] = interp(endpoint.wrap(decoded))\n\n  result.map(outputEncoder.encode(_))\n}\n")),(0,i.kt)("p",null,"That code is a little heavy and abstract, but there's really no way to avoid abstraction - after all, we need to be prepared for any and all models that our users might give us, so we need to be very abstract!"),(0,i.kt)("p",null,"To explain a little bit:"),(0,i.kt)("p",null,(0,i.kt)("inlineCode",{parentName:"p"},"FunctorAlgebra[Alg, IO]")," is ",(0,i.kt)("inlineCode",{parentName:"p"},"Alg[IO]")," (for a specific shape of ",(0,i.kt)("inlineCode",{parentName:"p"},"Alg"),"). This could be ",(0,i.kt)("inlineCode",{parentName:"p"},"HelloWorldService[IO]"),", if we knew the types (which we don't, because we're in the dynamic, runtime world).\nRelated to that, ",(0,i.kt)("inlineCode",{parentName:"p"},"FunctorInterpreter[Op, IO]")," is a different way to view an ",(0,i.kt)("inlineCode",{parentName:"p"},"Alg[IO]"),", which is as a higher-kinded function. See ",(0,i.kt)("a",{parentName:"p",href:"/smithy4s/docs/design/services#codifying-the-duality-between-initial-and-final-algebras"},"this document")," for more explanation."),(0,i.kt)("p",null,"The steps we're taking are:"),(0,i.kt)("ol",null,(0,i.kt)("li",{parentName:"ol"},"Find the endpoint within the service, using its operation name"),(0,i.kt)("li",{parentName:"ol"},"In ",(0,i.kt)("inlineCode",{parentName:"li"},"runEndpoint"),", decode the input ",(0,i.kt)("inlineCode",{parentName:"li"},"Document")," to the type the endpoint expects"),(0,i.kt)("li",{parentName:"ol"},"Run the interpreter using the decoded input"),(0,i.kt)("li",{parentName:"ol"},"Encode the output to a ",(0,i.kt)("inlineCode",{parentName:"li"},"Document"),".")),(0,i.kt)("p",null,"Let's see this in action with our actual service! But first, just for this guide, we'll define the routes for a fake instance of the server we're going to call:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'import org.http4s.HttpApp\nimport org.http4s.MediaType\nimport org.http4s.headers.`Content-Type`\nimport org.http4s.dsl.io._\n\nval routes = HttpApp[IO] { case GET -> Root / "weather" / city =>\n  Ok(s"""{"weather": "sunny in $city"}""").map(\n    _.withContentType(`Content-Type`(MediaType.application.json))\n  )\n}\n// routes: HttpApp[IO] = Kleisli(\n//   run = org.http4s.Http$$$Lambda$24027/0x0000000804928800@70408415\n// )\n')),(0,i.kt)("p",null,"Now we'll build a client based on the service we loaded earlier, using that route as a fake server:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'import org.http4s.client.Client\nimport smithy4s.http4s.SimpleRestJsonBuilder\n\n// first, we need some Service instance - we get one from the DynamicSchemaIndex we made earlier\nval service = dsi.getService(ShapeId("weather", "WeatherService")).get\n\nval client =\n  SimpleRestJsonBuilder(service.service)\n    .client(Client.fromHttpApp(routes))\n    .make\n    .toTry\n    .get\n')),(0,i.kt)("p",null,"And finally, what we've been working towards all this time - we'll select the ",(0,i.kt)("inlineCode",{parentName:"p"},"GetWeather")," operation, pass a ",(0,i.kt)("inlineCode",{parentName:"p"},"Document")," representing our input, and the client we've just built."),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'import cats.effect.unsafe.implicits._\n\nrun(\n  service = service.service,\n  operationName = "GetWeather",\n  input = Document.obj("city" -> Document.fromString("London")),\n  alg = client,\n).unsafeRunSync().show\n// res6: String = "{weather=\\"sunny in London\\"}"\n')),(0,i.kt)("p",null,"Enjoy the view! As an added bonus, because we happen to have this service at build-time, we can use the same method with a static, compile-time service:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'import weather._\n\nval clientStatic =\n  SimpleRestJsonBuilder(WeatherService)\n    .client(Client.fromHttpApp(routes))\n    .make\n    .toTry\n    .get\n// clientStatic: WeatherServiceGen[[I, E, O, SI, SO]IO[O]] = weather.WeatherServiceOperation$Transformed@2cfeb7\n\nrun(\n  service = WeatherService,\n  operationName = "GetWeather",\n  input = Document.obj("city" -> Document.fromString("London")),\n  alg = clientStatic,\n).unsafeRunSync().show\n// res7: String = "{weather=\\"sunny in London\\"}"\n')),(0,i.kt)("p",null,"Again, this is equivalent to the following call in the static approach:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'clientStatic.getWeather(city = "London").unsafeRunSync()\n// res8: GetWeatherOutput = GetWeatherOutput(weather = "sunny in London")\n')),(0,i.kt)("div",{className:"footnotes"},(0,i.kt)("hr",{parentName:"div"}),(0,i.kt)("ol",{parentName:"div"},(0,i.kt)("li",{parentName:"ol",id:"fn-1"},"That is, assuming they're written correctly to make no assumptions about the usecase.",(0,i.kt)("a",{parentName:"li",href:"#fnref-1",className:"footnote-backref"},"\u21a9")))))}d.isMDXComponent=!0}}]);