"use strict";(self.webpackChunksmithy4s=self.webpackChunksmithy4s||[]).push([[6672],{3905:(e,n,t)=>{t.d(n,{Zo:()=>d,kt:()=>m});var a=t(7294);function i(e,n,t){return n in e?Object.defineProperty(e,n,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[n]=t,e}function o(e,n){var t=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);n&&(a=a.filter((function(n){return Object.getOwnPropertyDescriptor(e,n).enumerable}))),t.push.apply(t,a)}return t}function r(e){for(var n=1;n<arguments.length;n++){var t=null!=arguments[n]?arguments[n]:{};n%2?o(Object(t),!0).forEach((function(n){i(e,n,t[n])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(t)):o(Object(t)).forEach((function(n){Object.defineProperty(e,n,Object.getOwnPropertyDescriptor(t,n))}))}return e}function s(e,n){if(null==e)return{};var t,a,i=function(e,n){if(null==e)return{};var t,a,i={},o=Object.keys(e);for(a=0;a<o.length;a++)t=o[a],n.indexOf(t)>=0||(i[t]=e[t]);return i}(e,n);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(a=0;a<o.length;a++)t=o[a],n.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(e,t)&&(i[t]=e[t])}return i}var l=a.createContext({}),c=function(e){var n=a.useContext(l),t=n;return e&&(t="function"==typeof e?e(n):r(r({},n),e)),t},d=function(e){var n=c(e.components);return a.createElement(l.Provider,{value:n},e.children)},u="mdxType",p={inlineCode:"code",wrapper:function(e){var n=e.children;return a.createElement(a.Fragment,{},n)}},g=a.forwardRef((function(e,n){var t=e.components,i=e.mdxType,o=e.originalType,l=e.parentName,d=s(e,["components","mdxType","originalType","parentName"]),u=c(t),g=i,m=u["".concat(l,".").concat(g)]||u[g]||p[g]||o;return t?a.createElement(m,r(r({ref:n},d),{},{components:t})):a.createElement(m,r({ref:n},d))}));function m(e,n){var t=arguments,i=n&&n.mdxType;if("string"==typeof e||i){var o=t.length,r=new Array(o);r[0]=g;var s={};for(var l in n)hasOwnProperty.call(n,l)&&(s[l]=n[l]);s.originalType=e,s[u]="string"==typeof e?e:i,r[1]=s;for(var c=2;c<o;c++)r[c]=t[c];return a.createElement.apply(null,r)}return a.createElement.apply(null,t)}g.displayName="MDXCreateElement"},4809:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>l,contentTitle:()=>r,default:()=>p,frontMatter:()=>o,metadata:()=>s,toc:()=>c});var a=t(7462),i=(t(7294),t(3905));const o={sidebar_label:"Unions and sealed traits",title:"Unions and sealed traits"},r=void 0,s={unversionedId:"codegen/unions",id:"codegen/unions",title:"Unions and sealed traits",description:"Smithy's union keyword allow to define a co-product, namely a piece of data that can take one form among a list of possibilities.",source:"@site/../docs/target/jvm-2.13/mdoc/04-codegen/02-unions.md",sourceDirName:"04-codegen",slug:"/codegen/unions",permalink:"/smithy4s/docs/codegen/unions",draft:!1,editUrl:"https://github.com/disneystreaming/smithy4s/edit/main/modules/docs/src/04-codegen/02-unions.md",tags:[],version:"current",sidebarPosition:2,frontMatter:{sidebar_label:"Unions and sealed traits",title:"Unions and sealed traits"},sidebar:"tutorialSidebar",previous:{title:"Nullable Values",permalink:"/smithy4s/docs/codegen/customisation/nullable-values"},next:{title:"Default Values",permalink:"/smithy4s/docs/codegen/default-values"}},l={},c=[{value:"Flattening of structure members",id:"flattening-of-structure-members",level:3},{value:"Regarding JSON encoding",id:"regarding-json-encoding",level:3},{value:"Tagged union",id:"tagged-union",level:4},{value:"Untagged union",id:"untagged-union",level:4},{value:"Discriminated union",id:"discriminated-union",level:4},{value:"Union Projections and Visitors",id:"union-projections-and-visitors",level:2},{value:"Projection Functions",id:"projection-functions",level:4},{value:"Visitors",id:"visitors",level:4}],d={toc:c},u="wrapper";function p(e){let{components:n,...t}=e;return(0,i.kt)(u,(0,a.Z)({},d,t,{components:n,mdxType:"MDXLayout"}),(0,i.kt)("p",null,"Smithy's ",(0,i.kt)("inlineCode",{parentName:"p"},"union")," keyword allow to define a co-product, namely a piece of data that can take one form among a list of possibilities."),(0,i.kt)("p",null,"This concept translates naturally to Scala sealed-traits (or Scala 3 enums), and ",(0,i.kt)("inlineCode",{parentName:"p"},"union")," are therefore generated as such."),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-kotlin"},"union MyUnion {\n  i: Integer\n  s: MyStructure\n  u: Unit\n}\n\nstructure MyStructure {\n  b: Boolean\n}\n")),(0,i.kt)("p",null,"Translates to :"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},"sealed trait MyUnion\nobject MyUnion {\n  case class ACase(a: Int) extends MyUnion\n  case class SCase(s: MyStructure) extends MyUnion\n  case object UCase extends MyUnion\n}\n")),(0,i.kt)("p",null,"As you can see, each member of the sealed-trait ends up generated as a ",(0,i.kt)("inlineCode",{parentName:"p"},"case class")," wrapping the type corresponding to what the union member points to in Smithy."),(0,i.kt)("p",null,"However, having a union member point to the ",(0,i.kt)("inlineCode",{parentName:"p"},"Unit")," shape in Smithy leads to the corresponding sealed-trait member being generated as a ",(0,i.kt)("inlineCode",{parentName:"p"},"case object"),"."),(0,i.kt)("h3",{id:"flattening-of-structure-members"},"Flattening of structure members"),(0,i.kt)("p",null,'Under certain conditions, Smithy4s offers a mechanism to "flatten" structure members directly as a member of the sealed trait.'),(0,i.kt)("p",null,"Head over to the page explaining code-gen ",(0,i.kt)("a",{parentName:"p",href:"/smithy4s/docs/codegen/customisation/adts"},"customisation")," for a detailed explanation."),(0,i.kt)("h3",{id:"regarding-json-encoding"},"Regarding JSON encoding"),(0,i.kt)("p",null,"Smithy4s does not rely on the classic automated derivation mechanisms to determine how unions should be encoded in JSON. Rather, the Smithy models dictates the encoding. Indeed, there are multiple ways to encode unions in JSON."),(0,i.kt)("p",null,"By default, the specification of the Smithy language hints that the ",(0,i.kt)("inlineCode",{parentName:"p"},"tagged-union")," encoding should be used. This is arguably the best encoding for unions, as it works with members of any type (not just structures), and does not require backtracking during parsing, which makes it more efficient."),(0,i.kt)("p",null,"However, Smithy4s provides support for two additional encodings: ",(0,i.kt)("inlineCode",{parentName:"p"},"discriminated")," and ",(0,i.kt)("inlineCode",{parentName:"p"},"untagged"),", which users can opt-in via the ",(0,i.kt)("inlineCode",{parentName:"p"},"alloy#discriminated")," and ",(0,i.kt)("inlineCode",{parentName:"p"},"alloy#untagged")," trait, respectively. These are mostly offered as a way to retrofit existing APIs in Smithy."),(0,i.kt)("h4",{id:"tagged-union"},"Tagged union"),(0,i.kt)("p",null,"This is the default behaviour, and happens to visually match how Smithy unions are declared. In this encoding, the union is encoded as a JSON object with a single key-value pair, the key signalling which alternative has been encoded."),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre"},"union Tagged {\n  first: String\n  second: IntWrapper\n}\n\nstructure IntWrapper {\n  int: Integer\n}\n")),(0,i.kt)("p",null,"The following instances of ",(0,i.kt)("inlineCode",{parentName:"p"},"Tagged")),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'Tagged.FirstCase("smithy4s")\nTagged.SecondCase(IntWrapper(42)))\n')),(0,i.kt)("p",null,"are encoded as such :"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-json"},'{ "first": "smithy4s" }\n{ "second": { "int": 42 } }\n')),(0,i.kt)("h4",{id:"untagged-union"},"Untagged union"),(0,i.kt)("p",null,"Untagged unions are supported via an annotation: ",(0,i.kt)("inlineCode",{parentName:"p"},"@untagged"),". Despite the smaller payload size this encoding produces, it is arguably the worst way of encoding unions, as it may require backtracking multiple times on the parsing side. Use this carefully, preferably only when you need to retrofit an existing API into Smithy"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-kotlin"},"use alloy#untagged\n\n@untagged\nunion Untagged {\n  first: String\n  second: IntWrapper\n}\n\nstructure IntWrapper {\n  int: Integer\n}\n")),(0,i.kt)("p",null,"The following instances of ",(0,i.kt)("inlineCode",{parentName:"p"},"Untagged")),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'Untagged.FirstCase("smithy4s")\nUntagged.SecondCase(Two(42)))\n')),(0,i.kt)("p",null,"are encoded as such :"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-json"},'"smithy4s"\n{ "int": 42 }\n')),(0,i.kt)("h4",{id:"discriminated-union"},"Discriminated union"),(0,i.kt)("p",null,"Discriminated union are supported via an annotation: ",(0,i.kt)("inlineCode",{parentName:"p"},'@discriminated("tpe")'),", and work only when all members of the union are structures.\nIn this encoding, the discriminator is inlined as a JSON field within JSON object resulting from the encoding of the member."),(0,i.kt)("p",null,"Despite the JSON payload exhibiting less nesting than in the ",(0,i.kt)("inlineCode",{parentName:"p"},"tagged union")," encoding, this encoding often leads to bigger payloads, and requires backtracking once during parsing."),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-kotlin"},'use alloy#discriminated\n\n@discriminated("tpe")\nunion Discriminated {\n  first: StringWrapper\n  second: IntWrapper\n}\n\nstructure StringWrapper {\n  string: String\n}\n\nstructure IntWrapper {\n  int: Integer\n}\n')),(0,i.kt)("p",null,"The following instances of ",(0,i.kt)("inlineCode",{parentName:"p"},"Discriminated")),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'Discriminated.FirstCase(StringWrapper("smithy4s"))\nDiscriminated.SecondCase(IntWrapper(42)))\n')),(0,i.kt)("p",null,"are  encoded as such"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-json"},'{ "tpe": "first", "string": "smithy4s" }\n{ "tpe": "second", "int": 42 }\n')),(0,i.kt)("h2",{id:"union-projections-and-visitors"},"Union Projections and Visitors"),(0,i.kt)("p",null,"In order to make working with unions more ergonomic, smithy4s provides projection functions and generates visitors for all unions."),(0,i.kt)("h4",{id:"projection-functions"},"Projection Functions"),(0,i.kt)("p",null,"Here we will see what a projection function looks like using a simple union example of ",(0,i.kt)("inlineCode",{parentName:"p"},"Pet"),"."),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},"sealed trait Pet {\n  object project {\n    def dog: Option[Dog]\n    def cat: Option[Cat]\n  }\n}\nobject Pet {\n  case class DogCase(dog: Dog) extends Pet\n  case class CatCase(cat: Cat) extends Pet\n}\n")),(0,i.kt)("p",null,"These functions can then be used as follows:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'val myPet: Pet = Pet.DogCase(Dog(name = "Spot"))\n\nmyPet.project.dog // Some(Dog(name = "Spot"))\nmyPet.project.cat // None\n')),(0,i.kt)("p",null,"These projection functions make it so you can work with specific union alternatives without needing to do any pattern matching."),(0,i.kt)("h4",{id:"visitors"},"Visitors"),(0,i.kt)("p",null,"Using the same pet example, we will now see what the visitors look like that smithy4s generates."),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},"sealed trait Pet {\n  def accept[A](visitor: Pet.Visitor[A]): A = // ...\n}\nobject Pet {\n  case class DogCase(dog: Dog) extends Pet\n  case class CatCase(cat: Cat) extends Pet\n\n  trait Visitor[A] {\n    def dog(dog: Dog): A\n    def cat(cat: Cat): A\n  }\n}\n")),(0,i.kt)("p",null,"Similar to the projection functions, the visitor allows us to handle the alternatives without a pattern match. For example:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'val myPet: Pet = Pet.DogCase(Dog(name = "Spot"))\n\nval visitor = new Pet.Visitor[String] {\n    def dog(dog: Dog): String = s"Dog named ${dog.name}"\n    def cat(cat: Cat): String = s"Cat named ${cat.name}"\n}\n\nmyPet.accept(visitor) // "Dog named Spot"\n')),(0,i.kt)("p",null,"You can also implement a Visitor using ",(0,i.kt)("inlineCode",{parentName:"p"},"Visitor.Default")," to provide a default value to be used for cases that you don't explicitly implement. For example:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala"},'val myPet: Pet = Pet.DogCase(Dog(name = "Spot"))\n\nval visitor = new Pet.Visitor.Default[String] {\n    def default: String = "default value"\n    def cat(cat: Cat): String = s"Cat named ${cat.name}"\n}\n\nmyPet.accept(visitor) // "default value"\n')))}p.isMDXComponent=!0}}]);