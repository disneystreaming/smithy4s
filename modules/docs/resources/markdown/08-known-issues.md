---
sidebar_label: Known Issues
title: Known Issues
---

Here is a list of known issues in upstream libraries, documented in case you encounter them.

## HttpUriConflict Validation - Open

Currently, the validator that checks for `HttpUriConflict` is overly constraining. This means that it currently reports conflicts between URIs where there is actually no conflict. For example, operations with the following two `http` traits currently report a conflict:

```smithy
@http(method: "GET", uri: "/hello")

@http(method: "GET", uri: "/{name}/greet")
```

There is an [issue open](https://github.com/awslabs/smithy/issues/1029) on the Smithy repository where you can track progress on this being resolved.
