---
title: Package Remapping
---

By default, Smithy4s generates Scala code using the Smithy namespace as the Scala package name verbatim. For example, a shape in namespace `com.example.api` will be placed in package `com.example.api`.

Package remapping lets you override this at render time (without modifying Smithy namespaces or ShapeIds) via the `smithy4sCodegen` model metadata key.

## Configuration

Add a `smithy4sCodegen` metadata entry to any Smithy file in your model:

```kotlin
$version: "2.0"

metadata smithy4sCodegen = {
    packagePrefix: "internal.generated",
    packageMappings: {
        "com.example.special": "explicit.pkg"
    },
    allowedNamespaces: [
        "com.example.*"
    ],
    excludedNamespaces: [
        "com.example.ignored"
    ]
}
```

All four fields are optional and can be combined freely.

### `packagePrefix`

Prepends a prefix to every generated package name:

```kotlin
metadata smithy4sCodegen = { packagePrefix: "internal.generated" }
```

A shape in namespace `com.example.api` will be placed in package `internal.generated.com.example.api`.

### `packageMappings`

Maps individual namespaces to explicit package names, overriding any `packagePrefix` for the matched namespace:

```kotlin
metadata smithy4sCodegen = {
    packageMappings: { "com.example.special": "explicit.pkg" }
}
```

A shape in namespace `com.example.special` will be placed in package `explicit.pkg`.

When both `packagePrefix` and `packageMappings` are present, an explicit mapping entry takes precedence over the prefix for any namespace it matches.

### `allowedNamespaces`

Restricts code generation to the listed namespaces. When set, only namespaces matching at least one of the patterns are processed. Accepts the same wildcard patterns as `excludedNamespaces` (see below).

```kotlin
metadata smithy4sCodegen = {
    allowedNamespaces: ["com.example.*"]
}
```

When combined with the build-tool `allowedNamespaces` setting (sbt/mill) or `--allowed-ns` (CLI), the two are unioned: a namespace is allowed if it matches *either* source. Use this Smithy-metadata-based form going forward — the build-tool settings are deprecated in favor of it.

### `excludedNamespaces`

Prevents code generation for the listed namespaces. Accepts the following wildcard patterns:

- `com.example.ignored` - exact match
- `com.example.*` - matches `com.example` followed by any additional segments
- `com.example*` - like above, but also matches `com.example` itself

```kotlin
metadata smithy4sCodegen = {
    excludedNamespaces: ["com.example.ignored", "com.internal.*"]
}
```

When combined with the build-tool `excludedNamespaces` setting (sbt/mill) or `--excluded-ns` (CLI), the two are unioned. The build-tool settings are deprecated in favor of this Smithy-metadata-based form.

## Cross-namespace references

When package remapping is active, references to types from other namespaces are remapped automatically. Generated import statements will use the remapped package name, so the compiled Scala code remains self-consistent.

For example, if `com.a` is remapped to `gen.com.a`, a structure in `com.b` that references a type from `com.a` will import it as `gen.com.a.MyType`.

### Cross-module remapping

This also works across module boundaries. When Smithy4s generates code for Module A with a `packagePrefix`, it writes the namespace-to-package mapping into the `smithy4sGenerated` tracking manifest:

```smithy
metadata smithy4sGenerated = [{
  namespaces: ["com.example.api"],
  renderedPackages: { "com.example.api": "gen.com.example.api" }
}]
```

When Module B depends on Module A's jar and runs codegen, Smithy4s picks up Module A's manifest from the classpath and applies its `renderedPackages` entries as additional package mappings before rendering Module B's code. Any reference to a type from `com.example.api` in Module B's generated code will be resolved to `gen.com.example.api` - matching the actual location of Module A's compiled classes.

Module B's own `smithy4sCodegen` mappings take precedence over any upstream manifest entries.

Note: `smithy4sCodegen` metadata itself **is** stripped from upstream jars (it starts with `smithy4s`), so each module defines only its own remapping. The manifest-based propagation described above is what carries remapping information downstream.

## Smithy namespace vs. Scala package

Package remapping is purely a render-time transformation. The underlying Smithy ShapeIds remain unchanged, so:

- The `smithy4sGenerated` manifest records original Smithy namespaces, preserving correct duplicate detection across multi-module builds.
- The generated `val id: ShapeId` values in Scala code still reflect the original Smithy namespace.
- Downstream tools that read the Smithy model (validators, OpenAPI generators, etc.) are unaffected.
