---
sidebar_label: Known Issues
title: Known Issues
---

Here is a list of known issues in upstream libraries, documented in case you encounter them.

## HttpUriConflict Validation - Closed

Historically, the validator that checks for `HttpUriConflict` was overly constraining. It may have reported conflicts between URIs where there is actually no conflict in older versions of Smithy. For example, operations with the following two `http` traits:

```smithy
@http(method: "GET", uri: "/hello")

@http(method: "GET", uri: "/{name}/greet")
```

There is an [closed issue](https://github.com/awslabs/smithy/issues/1029) on the Smithy which was merged as of July 2024.
