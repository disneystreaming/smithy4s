---
sidebar_label: Smithy build config
title: Smithy Build Configuration
---

## Introduction

Smithy provides the ability to configure the Smithy build and output by a [smithy-build configuration file](https://smithy.io/2.0/guides/smithy-build-json.html#smithy-build-json). As smithy4s uses its own build logic, it generally loads its configuration from elsewhere. However, limited support for build customization using a Smithy build configuration file is available. In particular, the [OpenAPI plugin](https://smithy.io/2.0/guides/model-translations/converting-to-openapi.html) can be used to customize the OpenAPI generation.

### Customizing OpenAPI generation via smithy build

In order to apply a custom OpenAPI config, you need a `smithy-build.json` file with the OpenAPI configuration, such as the following:

```json
{
  "version": "1.0",
  "plugins": {
    "openapi": {
      "service": "smithy.example#Weather",
      "version": "3.1.0",
      "jsonAdd": {
        "/info/title": "Replaced title value",
        "/info/nested/foo": {
          "hi": "Adding this object created intermediate objects too!"
        },
        "/info/nested/foo/baz": true
      }
    }
  }
}
```

This file can then used to configure codegen via the appropriate SBT setting:

```scala
Compile / smithyBuild := Some(baseDirectory.value / "smithy-build.json")
```

It can also be configured in Mill:

```scala
override def smithyBuild = Some(PathRef(millSourcePath / "smithy-build.json"))
```

Or, if you are using codegen directly via the command line tool, it can be passed via the argument `--smithy-build ./smithy-build.json`.

The generated OpenAPI should then have the configured transformations applied.