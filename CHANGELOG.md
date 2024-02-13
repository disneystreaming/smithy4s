# 0.18.9

* Supports error responses with `@httpResponseCode` fields.
* Fix in Bijection#identity which caused and infinite recursion, fixed in [1401](https://github.com/disneystreaming/smithy4s/pull/1401)

# 0.18.8

* Fix collision avoidance algorithm to cover Scala 3 keywords

# 0.18.7

* Added support for `@httpResponseCode` on newtypes (integer shapes that aren't exactly `smithy.api#Integer`), as well as refinements (e.g. ints with a `@range` constraint).

# 0.18.6

* If a Smithy trait, being a structure shape, had a Scala keyword in its member names, compilation of the generated would fail. In addition, enumeration values that matched a known keyword would have their name erroneously escaped with an underscore in the string literal.
These are now fixed in [#1344](https://github.com/disneystreaming/smithy4s/pull/1344).

* Smithy4s specific logic to extract manifest from jars should not run on jar. Fixed in [#1351](https://github.com/disneystreaming/smithy4s/pull/1351).

* In some concurrent scenarios, especially those of concurrent initialization of objects (e.g. tests), your application would previously be at risk of deadlocking due to [#537](https://github.com/disneystreaming/smithy4s/issues/537). This is now fixed by suspending evaluation of hints in companion objects using the `.lazily` construct: see [#1326](https://github.com/disneystreaming/smithy4s/pull/1326).

* Allow to configure how the default values (and nulls for optional fields) are rendered. Fixed in [#1315](https://github.com/disneystreaming/smithy4s/pull/1315)

# 0.18.5

* When encoding to `application/x-www-form-urlencoded`, omit optional fields set to the field's default value.

# 0.18.4

* Changes the behaviour of `Field#getUnlessDefault` and `Field#foreachUnlessDefault` to always take the value into consideration when the `smithy.api#required` trait
is present on the field. This leads to field values being always serialised even when their values match their defaults, as this abides by least-surprise-principle.

* Fix sbt `smithy4sUpdateLSPConfig` and mill `smithy4s.codegen.LSP/updateConfig` rendering of repositories.


# 0.18.3

* Support constraint traits on members targeting enums

Although it's weird to allow it, it is actually supported in Smithy.

* Tweak operation schema `*Input` and `*Output` functions

Some schema visitor will adjust their behaviour if a shape is the input or the output of an operation. For this reason we have a `InputOutput` class with a `Input` and `Output` hint that you can add to schemas to adjust the behaviour. `OperationSchema` has functions to work on input schemas and output schemas of an operation. This change makes these functions automatically add the relevant hint.

* OptionDefaultVisitor supports bijection

When the schema for the member of a structure is a bijection, and the structure is meant to be decoded from metadata (like http headers), optionality was disregarded. This was making optional member required when decoding.

* Fixing AwsInstanceMetadata codec in [#1266](https://github.com/disneystreaming/smithy4s/pull/1266)

Resolves an issue in which AWS credentials would be decoded using the wrong timestamp format, affecting AWS clients on EC2/ECS.

* Render explicit type annotations for some methods that were missing them in [#1272](https://github.com/disneystreaming/smithy4s/pull/1272)

This resolves a problem in which type inference would have different results between Scala 2.13 and 3.x, causing an error on Scala 2.13 under the `-Xsource:3` flag.

* Override `toString` on error shapes

Default `toString` implementation on `Throwable` prints the class name, instead, we decided to rely on a custom `toString` implementation.

# 0.18.2

## Expose UrlForm.parse and UrlFormDecodeError

In 0.18.0, support was added for `application/x-www-form-urlencoded` data. But, many of its related constructs were private, they are now public for users to access them directly.
https://github.com/disneystreaming/smithy4s/pull/1254


# 0.18.1

## Open enum support in Dynamic module

In 0.18.0, support was added for [open enums](https://disneystreaming.github.io/smithy4s/docs/codegen/customisation/open-enums) in smithy4s-generated code. This release extends that support to runtime (dynamic) schemas.

## Fixed a bug preventing a model pre-processor from being exercised

This model-preprocessor aims at removing constraints from output types in AWS specs (as AWS doesn't seem to respect said constraints)
https://github.com/disneystreaming/smithy4s/pull/1251

# 0.18.0

## Behavioural changes

The default timestamp format in Json serialisation is now `EPOCH_SECONDS`. This change is motivated by a desire to align with AWS and to improve
our compatibility with their tooling. Timestamps shapes (or members pointing to timestamp shapes) now will need to be annotated with `@timestampFormat("DATE_TIME")`
in order to retrieve the previous behaviour.

## Significant rewrite of the abstractions.

The abstractions that power smithy4s have been overhauled to facilitate integration with other protocols than simpleRestJson and other http libraries than http4s.
Many levels of the library has been impacted in significant ways, which is likely to break a great many third-party integrations. The amount of breaking
changes is too large to list exhaustively, therefore only a highlight is provided in this changelog.

* `smithy4s.schema.Field` is no longer a GADT differentiating from required/optional fields. There is now a `smithy4s.schema.Schema.OptionSchema` GADT member instead, which was required to support some traits.
* `smithy4s.schema.Schema.UnionSchema` now references an ordinal function, as opposed to the previous dispatch function.
* `smithy4s.Endpoint` now contains a `smithy4s.schema.OperationSchema`, which is a construct gathering all schemas related to an operation.
* `smithy4s.Service` now allows to get an ordinal value out of a reified operation, thus making it easier to dispatch it to the correct handler.
* `smithy4s.Service` now contains some methods for instantiation of services from an endpoint compilers.
* Two new packages in `core` have appeared : `smithy4s.server` and `smithy4s.client`, each containing protocol-agnostic constructs that aim at taking care of some of the complexity of integrating libraries/protocols with smithy4s.
* A `smithy4s.capability.MonadThrowLike` and `smith4s.capability.Zipper` types have been created, unlocking the writing of generic functions that benefits integrations
with various third-party libraries.
* `smithy4s.http.HttpRequest` and `smithy4s.http.HttpResponse` types have been created.
* `smithy4s.http.HttpUnaryClientCodecs` and `smithy4s.http.HttpUnaryServerCodecs` are new constructs that aim at facilitating the integration of http-protocols. In particular, they take care of a fair amount of complexity related to handling `smithy.api#http*` traits (including the reconciliation of data coming from http metadata and http bodies).
* Overall, the amount of code in the `smithy4s-http4s` module has drastically diminished, as the constructs necessary for the generalisation of the http-related logic have been created. We (maintainers) love http4s, and are not planning on publicly maintaining any other integration, but we are responsible for other integrations in our work ecosystem. Therefore, generalising what we can makes our jobs easier, but also should allow for third parties to have an easier time integrating their http-libraries of choice with Smithy4s.

### Highlight : schema partitioning

The most ground-breaking change of 0.18, which is crucial for how things are now implemented, is the addition of a `smithy4s.schema.SchemaPartition` utility that allow to split schemas into sub-schemas that each take care of the subset of the data. This mechanism allows to completely decouple the (de)serialisation of http bodies from the decoding of http metadata. This means, for instance, that JSON serialisation no longer has to be aware of traits such as `httpHeader`, `httpQuery`, `httpLabel`. This greatly facilitates the integration of other serialisation technologies (XML, URL Form, etc) as these no longer has to contain convoluted logic related to which fields should be skipped during (de)-serialisation.

As a result, the **smithy4s-json** module has been rewritten. In particular, the code it contains is now held in the `smithy4s.json` package, since it is no longer coupled with http-semantics. The `smithy4s.json.Json` object has also been created to provide high-level methods facilitating the encoding/decoding of generated types into json, which is helpful for a number of usecases that fall out of the server/client bindings.

## Features

### AWS SDK support.

Smithy4s' coverage of the AWS protocols has increased drastically. Now, the vast majority of services and operations are supported. This does mean that Smithy4s can effectively be used as a cross-platform AWS SDK (with caveats), delegating to `http4s` for transport.

The Smithy4s build plugins now also come with utilities to facilitate the code-generation from AWS service specifications.

Please refer yourself to the relevant [documentation page](https://disneystreaming.github.io/smithy4s/docs/protocols/aws/aws).

### Build plugins

Smithy has support in IDE via the smithy-language-server. The language server uses a configuration file to understand your project. In 0.18, our build plugins for `sbt` and `mill` can generate that configuration file for you. Use the following commands depending on the build tool you use, for `sbt`: `sbt smithy4sUpdateLSPConfig` and for `mill`: `mill smithy4s.codegen.LSP/updateConfig`.


### Mill

The `mill` plugin is build for version `0.11.0`. The changes to the API are solely results of this migration.

The most important migration bits:

1. Change from `def smithy4sInputDirs: mill.define.Sources` to `def smithy4sInputDirs: mill.define.Target[Seq[PathRef]]`
2. Change from `def manifest: T[mill.modules.Jvm.JarManifest]` to `def manifest: T[mill.api.JarManifest]`
3. Change from `def smithy4sAllExternalDependencies: T[Agg[Dep]]` to `def smithy4sAllExternalDependencies: T[Agg[BoundDep]]`

### Cats Module

Addition of a cats module to contain `SchemaVisitor` implementations of commonly-used cats typeclasses. Currently included are `cats.Show` and `cats.Hash` (note that `cats.Eq` is provided by the `cats.Hash` implementation).

See https://github.com/disneystreaming/smithy4s/pull/921

### Structure Patterns

Allows for marking string types with the `alloy#structurePattern` trait. This trait indicates that a given pattern, with parameters, applies to a given string and that this string should
actually be parsed into a structure where the members of the structure are derived from the parameters in the string pattern.

See https://github.com/disneystreaming/smithy4s/pull/942

### Non-Orphan Typeclass Instances

Allows creating implicit typeclass instances in the companion objects in the smithy4s-generated code. This is useful for creating instances of
typeclasses such as `cats.Show`, `cats.Eq` or any others you may be using.

See https://github.com/disneystreaming/smithy4s/pull/912

### smithy4s.Blob

`smithy4s.ByteArray` has been deprecated in favor of `smithy4s.Blob`. This new type is more flexible, in that it can be backed by byte arrays and byte buffers alike.
Additionally, it allows for O(1) concatenation. This change is motivated by a desire to ease integration with third party libraries whilst reducing the need of copies of binary data.

### Smithy4s Optics Instances

When the smithy4sRenderOptics setting is enabled, Lenses and Prisms will be rendered in the companion objects of the generated code when appropriate.

See https://github.com/disneystreaming/smithy4s/pull/1103

### Open Enumerations

Introduces alternative code generation for enums and intEnums when they are marked with the `alloy#openEnum` trait.

See https://github.com/disneystreaming/smithy4s/pull/1137

### Union Projections and Visitors

Added convenient methods for working with unions including projectors for each union alternative and a visitor in union companion objects that can be passed to each union's new `accept` method.

See https://github.com/disneystreaming/smithy4s/pull/1144

### Sparse collections

The `sparse` trait is now supported, allowing for the modelling of collections with null values. Its presence leads to the code-generation of `List[Option[A]]` and `Map[String, Option[A]]`.

See https://github.com/disneystreaming/smithy4s/pull/993

### Xml support

The `smithy4s-xml` now exists, containing utilities to parse XML blobs into the generated data classes, and render XML from the generated data classes. This serde logic abides by the rules described in the the official [smithy documentation](https://smithy.io/2.0/spec/protocol-traits.html?highlight=xml#xml-bindings).

### application/x-www-form-urlencoded support

The `smithy4s-core` now contains utilities to parse [application/x-www-form-urlencoded](https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST) payloads into the generated data classes, and render those payloads same payloads the generated data classes. This encoding allows for a few customisation, driven by [alloy traits](https://github.com/disneystreaming/alloy#alloyurlformflattened).

See https://github.com/disneystreaming/smithy4s/pull/1113

# 0.17.20

* Add empty line separating generated case classes from their companion objects in [#1175](https://github.com/disneystreaming/smithy4s/pull/1175)

# 0.17.19

This release brings in a smithy-model update, which resolves some issues that would've prevented code generation from succeeding. [#1164](https://github.com/disneystreaming/smithy4s/pull/1164)

# 0.17.18

Fixes a `ClassCastException` in document encoding in [#1161](https://github.com/disneystreaming/smithy4s/pull/1161)

# 0.17.17

More permissive test for HEAD requests in [#1157](https://github.com/disneystreaming/smithy4s/pull/1157)

# 0.17.16

Fixes a bug where HEAD responses contained an empty {} in the body (where there should be no body present). [#1149](https://github.com/disneystreaming/smithy4s/pull/1149)

# 0.17.15

Updates to fix compile errors when intEnum shapes are used as traits. [#1139](https://github.com/disneystreaming/smithy4s/pull/1139)

# 0.17.14

* Only transform AWS shapes named after standard shapes in [#1127](https://github.com/disneystreaming/smithy4s/pull/1127)
* Fixes AWS AwsStandardTypesTransformer bug by in [1129](https://github.com/disneystreaming/smithy4s/pull/1129)

# 0.17.13

* Backports Service interpreter logic introduced in [#908](https://github.com/disneystreaming/smithy4s/pull/908).
* Fixes rendering of deprecated annotations in mixins in [#1123](https://github.com/disneystreaming/smithy4s/pull/1123)

# 0.17.12

* Remove reserved types in https://github.com/disneystreaming/smithy4s/pull/1052

Remove a legacy mechanism of dealing with name conflicts in generated types. Fixes [#1051](https://github.com/disneystreaming/smithy4s/issues/1051)

* Flatten AWS newtypes in https://github.com/disneystreaming/smithy4s/pull/1110

Adjusts the rendering of Smithy shapes from AWS specs, as it would've often been inconvenient due to the change above.

* Bump webjar dependency to 0.47 in https://github.com/disneystreaming/smithy4s/pull/1100

Updates a previously frozen dependency to alleviate potential security issues.

# 0.17.11

This is mostly a bugfix and cleanup release.

* [aws] Keep casing in file credential provider in https://github.com/disneystreaming/smithy4s/pull/1076

Resolves a case-sensitivity issue in the file-based AWS credentials provider.

* Deprecate `ClientBuilder.use`, add `.make` in https://github.com/disneystreaming/smithy4s/pull/1073

Deprecates a method - the migration path would be just to move to another one with the same shape.

* Error transformations as middleware in https://github.com/disneystreaming/smithy4s/pull/1084

Changes the error transformation logic in the http4s servers so that it's implemented using the (public) per-endpoint middleware construct.

# 0.17.10

* Revert original behavior where middleware get all errors in https://github.com/disneystreaming/smithy4s/pull/1034

This change adds a fix for an accidental behavior change around error handling/capture in middlewares.

## Other changes

* Adding a comment in flatMapErrors in https://github.com/disneystreaming/smithy4s/pull/1030

# 0.17.9

* Update smithy-model to 1.31.0, alloy to 0.2.2 in https://github.com/disneystreaming/smithy4s/pull/1022

# 0.17.8

* backport of [improve: fallback unspecified members of deprecated trait to N/A] in https://github.com/disneystreaming/smithy4s/pull/989
* Dynamic module guide in https://github.com/disneystreaming/smithy4s/pull/960
* Add an option to encode missing fields as nulls in https://github.com/disneystreaming/smithy4s/pull/995

# 0.17.7

Make sure error handling logic in routing is applied before and after middleware application .

* Add course link to learning resources in https://github.com/disneystreaming/smithy4s/pull/965
* Http4s: pre- and post-error handling middleware in https://github.com/disneystreaming/smithy4s/pull/877

# 0.17.6

This release is backward binary-compatible with the previous releases from the 0.17.x lineage.

## Bug fixes

* Fixes a [bug](https://github.com/disneystreaming/smithy4s/pull/898) related to swagger-ui redirects that would occur with empty paths.
* Fixes a [bug](https://github.com/disneystreaming/smithy4s/pull/904) related to the undocumented "dynamic" module not respecting the order of fields specified in smithy models

# 0.17.5

This release is backward binary-compatible with the previous releases from the 0.17.x lineage.
However, the generated code produced by this version is not entirely source-compatible with the previous version.
More info below.

## Possible breaking changes

This version introduces changes in how the code is rendered that may result in some breakage in userland. We've
carefully architected the changes to reduce the likelihood of breakage happening.

A number of constructs are now rendered not in the companion object of the generated service, but rather in the companion
object of the reified operation type. These constructs include the error types that get generated by operations.

This change has been performed in order to eliminate the risk of collision between the generated types and some type
members present in the `smithy4s.Service` interface. This collision, when it happened, was making the code impossible to
compile.

In order to reduce breakage around the error types (which are likely to be used in userland), we have decided to generate
aliases at the location where they used to live. The generated code should not break source compatibility in the large
majority of usage of Smithy4s.

A small minority of users may have to change how they access the generated constructs they may have depended on. **This is unlikely**,
as the constructs in question are used internally by interpreters via interfaces that haven't changed, and they are not constructs
that are advertised in our documentation. We only expect some possible breakage in advanced usage performed by a handful of
people.

See:
* https://github.com/disneystreaming/smithy4s/pull/859
* https://github.com/disneystreaming/smithy4s/pull/848
* https://github.com/disneystreaming/smithy4s/pull/847

## Behavioural changes

### Adjust encoding/decoding HTTP query parameters

Changed the handling of the `httpQueryParams` (plural) trait so that possible `httpQuery`-annotated fields do not take priority
over it **during decoding**. This means that `httpQueryParams` receive the whole set of query parameters, which may induce duplication
with the value contained by overlapping `httpQuery`-annotated fields.

On the encoding side, the behaviour is that `httpQuery` fields have priority over `httpQueryParams` fields.

This is a more faithful implementation of [the spec](https://smithy.io/2.0/spec/http-bindings.html?highlight=httpquery#httpqueryparams-trait).

See https://github.com/disneystreaming/smithy4s/pull/827


## Improvements

### Validate model for codegen after transformations

Adds logic to validate the model after pre-processing model transformations (before the code-generation)

See https://github.com/disneystreaming/smithy4s/pull/821

### Support time zones in `DATE_TIME` parsing

AWS has changed the Smithy specification of the `DATE_TIME` timestamp format to precise that numeric offsets should be handled.
This is now the case.

See https://github.com/disneystreaming/smithy4s/pull/844

### Dynamic: Add `metadata` method

The currently undocumented `dynamic` module has received an improvement allowing to access the metadata
of the loaded models via its platform-agnostic interface.

See https://github.com/disneystreaming/smithy4s/pull/823


## Bug fixes

### Http4s client body

Empty bodies are now correctly using the built-in `withEmptyBody` of Http4s, which correctly removes
the `Content-Type` header from the request upon usage. This solves issues when Smithy4s is being called
(or calling) strict clients/servers that check this type of thing.

See https://github.com/disneystreaming/smithy4s/pull/826

### Handle NaN and Infinity in AWS JSON codecs

The AWS Json protocols specify that `NaN`, `Infinity` and `-Infinity` are valid values for `Double` and `Float` types.
This is now handled.

See https://github.com/disneystreaming/smithy4s/pull/822

### Better handling of special characters when loading Smithy models from dependencies

A bug was preventing dependencies that would have special characters in their absolute paths to be loaded successfully.
This is now fixed.

See https://github.com/disneystreaming/smithy4s/pull/850

### Http4s client: Support `Byte` parameters in paths

`Byte` fields are now correctly supported when used by an `httpLabel` member.

See https://github.com/disneystreaming/smithy4s/pull/819

# 0.17.4

This release is backward binary-compatible with the previous releases from the 0.17.x lineage.

## Improvements

### More efficient Json parsing of ArraySeq

See https://github.com/disneystreaming/smithy4s/pull/806

### Fix parsing logic of AWS credentials file to allow for comments

See https://github.com/disneystreaming/smithy4s/pull/811

### Add documentation on how to point AWS clients to local environments

See https://github.com/disneystreaming/smithy4s/pull/812

# 0.17.3

This release is backward binary-compatible with the previous releases from the 0.17.x lineage.

## User-facing features

### Addition of an new `@adt` trait to streamline the inlining of structures as sealed-trait members

Under certain conditions, it is now possible to annotate union shapes with `@adt`, which has the effect of inlining
all the structure shapes under it directly in the companion object of the union, as opposed to create `Case`-suffixed
wrappers. Additionally, when a union is annotated with `@adt`, the intersection of mixin shapes that are applied to every member of the union is now used as Scala-level mixin traits. This facilitates object-oriented usage of Smithy4s.

Read the new docs for more info.

See https://github.com/disneystreaming/smithy4s/pull/787

### Scaladoc now gets generated

Smithy `@documentation` traits (which has syntactic sugar in the form of triple-slashes-prefixed comments) is now used to generate Scaladoc above the relevant data-types, interfaces and methods that get generated in Smithy4s.

https://github.com/disneystreaming/smithy4s/pull/731

Thank you @zetashift for this awesome contribution and @msosnicki for this valuable contribution !

### Scala 3 wildcards now get generated when relevant.

Under conditions which should automatically be propagated from the build-tool to the code-generation,
Scala 3 wildcards now get generated instead of Scala 2 wildcards. This makes user experience better on
Scala 3, as syntax deprecation warnings will no longer be issued.

Thank you @albertpchen for this awesome contribution !

See https://github.com/disneystreaming/smithy4s/pull/736

### Simpler AWS clients

It is now possible to directly instantiate AWS clients against a Monadic context, which makes for a better
UX when calling unary request/response operations. When using that mode, stream operations being called
such clients will fail with a raised exception.

See https://github.com/disneystreaming/smithy4s/pull/744

### AWS config file credentials providers

It is now possible to load credentials from an AWS-compliant configuration file (typically found under ~/.aws/credentials).
This is wired by default in the clients, and has lower precedence than the other providers.

### Improve docs

We've improved and added new sections to the documentation, in particular around AWS SDK usage and model pre-processing.
## Bug fixes

### Null default value traits are now correctly handled

The default trait allows for not setting a value. Now, the absence of value (ie null) in the
default trait at the smithy level translates to the correct "zero" value of the target type.

See https://github.com/disneystreaming/smithy4s/pull/782

### Decoding Document.DNull to Optional fields now works correctly

Null documents were not being decoded as `None`, but rather were leading to decoding failures
when decoding data types from `smithy4s.Document`

See https://github.com/disneystreaming/smithy4s/pull/725

### Fix the JS source-map URI

The URI was previously using the wrong relative path

See https://github.com/disneystreaming/smithy4s/pull/740

### Traits applied on collection members now leads to hints being correctly generated

See https://github.com/disneystreaming/smithy4s/pull/769

### Defaults are not ignored in refinements

Loading a smithy 1.0 model with smithy 2.0 tooling (which Smithy4s uses) leads to the automatic
addition of "default" traits on some shapes and members. When combined with refinements, this had
the side effect of treating the refined type as required when it should be in fact optional.
It's all the more confusing that there is no mechanism in place to reconcile refinement logic,
with default values, as refinement logic is expressed in run-time code whereas default value validation
is expressed in build-time code.

See https://github.com/disneystreaming/smithy4s/pull/795

## Other notable changes

### Performance improvements of the json parsing logic

Yet another awesome contribution from @plokhotnyuk to shave allocations off the Json parsing logic, leading
to performance improvements.

See https://github.com/disneystreaming/smithy4s/pull/764

### Compliance tests

Our implementation of our `alloy#simpleRestJson` protocol is now derived automatically from test specifications
written in [Smithy itself](https://github.com/disneystreaming/alloy/tree/main/modules/protocol-tests/resources/META-INF/smithy)

See:
* https://github.com/disneystreaming/smithy4s/pull/715
* https://github.com/disneystreaming/smithy4s/pull/747

This also paves the road for testing our implementation of the AWS protocols using official tests, which are located
[there]

### Generic logic against smithy4s-generated enumerations is now easier to write

Some tweaks were made to the `smithy4s.Enumeration.Value` interface to allow for more generic logic using enumerations.

See https://github.com/disneystreaming/smithy4s/pull/794

# 0.17.2

This release is backward binary-compatible with the previous releases from the 0.17.x lineage.

## User-facing features

### Scala 3 unions support for operation errors

See https://github.com/disneystreaming/smithy4s/pull/707

In order to render Operation errors as Scala 3 union types, a following metadata flag needs to be added: `metadata smithy4sErrorsAsScala3Unions = true` (in any of the smithy files that are used for code generation).

### Source-mapping github paths are now automatically added during scala-js compilation

This will make it easier to run front-end debuggers on webpage build with smithy4s-issued clients

See https://github.com/disneystreaming/smithy4s/pull/706

### Addition of Transformation.apply utilty method :

it's now possible to invoke transformations more conveniently in polymorhic code, via a method in the `Transformation` companion object

See https://github.com/disneystreaming/smithy4s/pull/681

## Bug fixes

### Static query params are now handled correctly

It is now possible to define static query parameters when using the http trait :

```smithy
@http(method: "GET", uri: "/foo?bar=baz)
operation Foo {}
```

### Service interfaces now receive the set of all operations tied to resources transitively tied to the service

For instance, when running the code-generator, the `Library` interface will now receive a `getBook` method, which
wasn't previously the case

```smithy
service Library {
  resources: [Book]
}

resource Book {
  read: GetBook
}

@readonly
operation GetBook {
}
```

### Other fixes and improvements :

See https://github.com/disneystreaming/smithy4s/pull/689

* Various codegen fixes and improvements  by @Baccata in https://github.com/disneystreaming/smithy4s/pull/677
* fix for timestamp format issue for aws tests by @yisraelU in https://github.com/disneystreaming/smithy4s/pull/675
* Make whitespace around colons consistent by @kubukoz in https://github.com/disneystreaming/smithy4s/pull/682
* Add resource operations to generated service by @Baccata in https://github.com/disneystreaming/smithy4s/pull/686
* fix path segment parsing when suffixed with query by @yisraelU in https://github.com/disneystreaming/smithy4s/pull/689
* Compliancetests fixes improvements by @yisraelU in https://github.com/disneystreaming/smithy4s/pull/680
* restructured timeout call and attemptNarrow by @yisraelU in https://github.com/disneystreaming/smithy4s/pull/708
* [compliance tests] addresses timeouts on the server side  by @yisraelU in https://github.com/disneystreaming/smithy4s/pull/712
* Fix ShapeId.parse not working for valid shapes by @kubukoz in https://github.com/disneystreaming/smithy4s/pull/714
* codegen cli should use a non-zero exit code when failing by @daddykotex in https://github.com/disneystreaming/smithy4s/pull/713



# 0.17.0

This 0.17.0 release of Smithy4s brings a number of improvements on the abstractions implemented by the generated code, in particular in terms of flexibility and user experience.

This release also aims at bringing inter-operability with other tools and projects that Disney Streaming is putting forward to reinforce the Smithy ecosystem, such as [smithy-translate](https://github.com/disneystreaming/smithy-translate/) and [alloy](https://github.com/disneystreaming/alloy).

In order to achieve these improvements, we've had to break a number of things at different levels. This release is therefore neither source nor binary compatible with the previous ones, and also forces the user to update their Smithy specifications.

## Breaking changes

### Smithy-level breaking changes

See https://github.com/disneystreaming/smithy4s/pull/561

The Smithy shapes that were previously residing under `smithy4s.api` namespace have moved to the `alloy` namespace. Alloy is a standalone library containing Smithy shapes and validators, defined [here](https://github.com/disneystreaming/alloy).

The reason for us to bring this change is to have a language specific location to define shapes that are relevant to the protocols/runtime-behaviours we're putting forward, that could be used by tooling working with other languages than Scala. It was important for us to lose the `4s` suffix, which is short for `for Scala`.

This change implies, for instance, that any use of `smithy4s.api#simpleRestJson` in your specification will have to be replaced by `alloy#simpleRestJson`.

Note that this change, in use cases that follow our documentation, should have no visible effect in the Scala code.

### Build-plugins breaking changes (SBT/mill)

#### Multiple input directories

See https://github.com/disneystreaming/smithy4s/pull/587

The `smithy4sInputDir` setting/task in SBT/mill has been replaced by `smithy4sInputDirs`, allowing the user to set several directories where the plugins should look for Smithy files.

#### Change in smithy-library dependency resolution

See https://github.com/disneystreaming/smithy4s/pull/607

We've changed the smithy-sharing mechanism to do two things:

1. By default, any dependency declared "normally" in SBT or mill, by means or `libraryDepedencies ++=` or `def ivyDeps`, will be inspected for Smithy files after being resolved. This means that, for instance, if your application has a runtime dependency on a library that was built with Smithy4s and contains Smithy files, your local specs can use the code defined in these Smithy files to create or annotate new shapes. You no longer need to declare those using `% Smithy4s` or `def smithy4sIvyDeps`: these are now reserved for libraries containing Smithy files that you **do not want your application's runtime to depend on**.
2. Libraries built by Smithy4s automatically track the dependencies that they used during their own code-generation, by storing some metadata in their Jar's manifests. By default, the Smithy4s plugins will also pull those dependencies (which will have been declared upstream using `% Smithy4s` in SBT or `def smithy4sIvyDeps` in mill), for your project's code generation. This facilitates the transitivity of specification-holding artifacts. This mechanism is used, for instance, to communicate to users projects the fact that Smithy4s depends on shapes that are defined in the [alloy](https://github.com/disneystreaming/alloy) library, and that these shapes should be made available to user projects, without impacting the user's application runtime, and without requiring any setup from the user.

### Normal-usage breaking changes in the generated code

See https://github.com/disneystreaming/smithy4s/pull/599

Depending on your setup, it may be a breaking change, but `@deprecated` Smithy-traits now translate to the `@deprectated` Scala annotation in the generated code. For instance, if you used `@enum` heavily, you'll probably deprecation warnings in your console when compiling. Depending on your `scalacOptions`, it is possible that these warnings turn into errors. If you want to silence these particular errors while upgrading, you can do the following:

```sbt
scalacOptions ++= Seq(
  "-Wconf:msg=object Enum in package api is deprecated:silent",
  "-Wconf:msg=type Enum in package api is deprecated:silent",
  // for Scala 3
  "-Wconf:msg=object Enum in package smithy.api is deprecated:silent",
  "-Wconf:msg=type Enum in package smithy.api is deprecated:silent"
)
```

### Normal-usage source breaking changes

See https://github.com/disneystreaming/smithy4s/pull/569

If you use Smithy4s in the ways that were previously advertised in the documentation, you may have to perform some small adjustments.

In particular, the `simpleRestJson` extension method that was added to implementations of service-interfaces generated by Smithy4s is now removed, in favour of the `SimpleRestJsonBuilder` construct (which now works for any `service` Smithy shape that will have been annotated with `alloy#simpleRestJson`).

Additionally, some methods that were deprecated in 0.16.x releases [have been removed](https://github.com/disneystreaming/smithy4s/pull/589).

### Advanced usage breaking changes

The abstractions that the generated code implements and that the runtime interpreters use have undergone some massive changes.

Non-exhaustive list of symbol renames :

| old                               | new                               |
| --------------------------------- | --------------------------------- |
| smithy4s.Monadic                  | smithy4s.kinds.FunctorAlgebra     |
| smithy4s.Interpreter              | smithy4s.kinds.FunctorInterpreter |
| smithy4s.Service#asTransformation | toPolyFunction                    |
| smithy4s.Service#transform        | fromPolyFunction                  |
| smithy4s.PolyFunction             | smithy4s.kinds.PolyFunction       |
| smithy4s.Transformation           | smithy4s.kinds.PolyFunction5      |
| smithy4s.GenLift[F]#λ             | smithy4s.kinds.Kind1[F]#toKind5   |

#### Unification of the natural-transformations/polymorphic functions.

* Smithy4s makes a lot of use of polymorphic functions of various kinds. Those are now code-generated (see the `project/Boilerplate.scala` file) to ensure the consistency of the various ways they are being used. This means that `smithy4s.PolyFunction` has moved to `smithy4s.kinds.PolyFunction`, and that the previous `smithy4s.Transformation` is now `smithy4s.kinds.PolyFunction5`. This decision ripples in the `smithy4s.Service` interface, which now sports `toPolyFunction` and `fromPolyFunction` methods, allowing to turn a finally-encoded implementation into a final one. `smithy4s.kinds.PolyFunction2` is also a thing, and may be used in bi-functor contexts.
* `kind`-specific types were created to facilitate the "lift" of constructs to the right kinds. For instance, when inspecting the internals of this library, you might see things like `Kind1[IO]#toKind5` where it was previously `GenLift[IO]#λ`. We're hoping to convey meaning better, although this code is definitely still not trivial (and never will).
* `smithy4s.Transformation` is now a typeclass-like construct, which expresses the fact that a construct can be applied like a function. This construct is used by the `transform` method that is generated on service interfaces, which allows to apply custom behaviour generically on all method invocations in these interfaces.
* The `Service` interface takes a single `Alg` type parameter, the `Op` parameter has moved to type-member position, facilitating implicit search in some contexts (as well as the writing of some logic).
* A bunch of path-dependent type aliases were created in the `Service` interface.
* The `compliancetest` module has changed drastically in UX. For those not aware, this module allows to run tests written in Smithy against your own implementation of protocols. This will be useful for third-party libraries that implement `simpleRestJson` (or any other http/rest like protocol), to virtually get tests for free. We don't think this module had any users so far, but we'll slowly be porting some of our tests away from the `smithy4s` repository and into the `alloy` repository.

## User facing improvements

### Stubs

See https://github.com/disneystreaming/smithy4s/pull/595

It is now possible to quickly stub a service with a default value (`IO.stub` being a good candidate), which can be helpful for testing purposes. The resulting code looks like this :

```scala
import smithy4s.hello._
import cats.effect._
val stubbedHelloWorld: HelloWorldService[IO] = new HelloWorldService.Default[IO](IO.stub)
```

### Transformations, including bi-functors

See https://github.com/disneystreaming/smithy4s/pull/584

`smithy4s.Transformation`  has been revised to facilitate the integration with various shapes of transformations. It allows, in particular, to transform a service implementation by applying generic (but polymorphic) behaviour in all of its methods. For instance, this can be used to apply a timeout on all of the methods of a service, or retrying behaviour, etc ...

In particular, the `smithy4s.Transformation` companion object contains in particular `AbsorbError` and `SurfaceError` interfaces that developers can leverage to get their services to go from mono-functor (where all errors are treated as `Throwable`) to bi-functor (where errors are surfaced on a per-endpoint basis, forcing the developers to handle them one way or another), and vice-versa.

### Bi-functor-specialised type aliases

See https://github.com/disneystreaming/smithy4s/pull/584/files#diff-064c6fb10e5927021c4fdb928e68fd8594443b767c54bec7d3b4a424e087401bR26

The generated code now contains bi-functor-specialised `ErrorAware`type-aliases. Those, combined with the transformations described above, should make it easier to interop with Bi-functor constructs such as `EitherT` or `ZIO`.

### Endpoint Specific Middleware

See https://github.com/disneystreaming/smithy4s/pull/614

Adds the ability to have smithy4s-level middleware that is made aware of the `Server` and `Endpoint` for use in creating middleware implementations. This unlocks creating middleware that is aware of the Smithy traits (`Hints` in smithy4s) and shapes in your specification. This means the middleware can apply transformations based on traits applied in a Smithy specification and it can return error responses defined in the Smithy specification. An example of this is authentication. You are now able to create middleware that will check authentication on only the endpoints that require it AND you can return a smithy-defined error response when the authentication is not valid. See the [endpoint specific middleware guide](https://disneystreaming.github.io/smithy4s/docs/guides/endpoint-middleware) for more.


### Error Response Handling Improvements

See https://github.com/disneystreaming/smithy4s/pull/570

Streamlines and improves how error responses are mapped to their corresponding smithy4s-generated types. It now works such that IF no `X-Error-Type` header is found AND the status code doesn't map precisely to an error annotated with `@httpCode` AND exactly one error happens to have `@error("client")` without `@httpCode`, that error will be selected (provided the status code is in the 4xx range). Same for `@error("server")` and 5xx range. See the [error handling documentation](https://disneystreaming.github.io/smithy4s/docs/protocols/simple-rest-json/client#error-handling) for more.

### Support for more HTTP methods

Previously, smithy4s's `HttpEndpoint` was limited to supporting just a small subset of HTTP methods (`POST`, `GET`, `PATCH`, `PUT` and `DELETE`). This is now mitigated, and all other methods are accepted by `HttpEndpoint`, by means of an open-enumeration.

### Configurable maximum arity during Json parsing

See https://github.com/disneystreaming/smithy4s/pull/569

In order to mitigate known security problems, our json parsing logic has hard-limits over the number of elements it will parse from arrays
or maps, resulting in an error when receiving payloads with larger collections. Previously, this limit was hardcoded to 1024 elements per collection. This is now configurable, 1024 being the default.

### Polymorphic refinements

See https://github.com/disneystreaming/smithy4s/pull/649

Refinements applied on list/map shapes can now produce parameterised types. This allows, for instance, to have generic
refinements on `list` shapes that produce `cats.data.NonEmptyList` containing the same types of elements.
