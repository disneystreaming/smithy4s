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
    excludedNamespaces: [
        "com.example.ignored"
    ]
}
```

All three fields are optional and can be combined freely.

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

### `excludedNamespaces`

Prevents code generation for the listed namespaces. Accepts the same wildcard patterns as the `allowedNamespace` / `excludedNamespace` codegen arguments:

- `com.example.ignored` - exact match
- `com.example.*` - matches `com.example` followed by any additional segments
- `com.example*` - like above, but also matches `com.example` itself

```kotlin
metadata smithy4sCodegen = {
    excludedNamespaces: ["com.example.ignored", "com.internal.*"]
}
```

## Cross-namespace references

When package remapping is active, cross-namespace `Type.Ref` nodes inside declarations are remapped automatically. Generated import statements will use the remapped package name, so the compiled Scala code remains self-consistent.

For example, if `com.a` is remapped to `gen.com.a`, a structure in `com.b` that references a type from `com.a` will import it as `gen.com.a.MyType`.

## Smithy namespace vs. Scala package

Package remapping is purely a render-time transformation. The underlying Smithy ShapeIds remain unchanged, so:

- The `smithy4sGenerated` manifest records original Smithy namespaces, preserving correct duplicate detection across multi-module builds.
- The generated `val id: ShapeId` values in Scala code still reflect the original Smithy namespace.
- Downstream tools that read the Smithy model (validators, OpenAPI generators, etc.) are unaffected.

## Scope

`smithy4sCodegen` metadata is stripped from upstream jars by `ModelLoader` (it starts with `smithy4s`), so each project applies its own remapping independently. This makes it safe to use different package structures in different modules of the same multi-module build.
