---
sidebar_label: Protocols and Smithy4s
title: Protocols and Smithy4s
---

The code generated by Smithy4s is strictly **protocol agnostic**, meaning that there is no particular processing to handle HTTP semantics, or JSON semantics in the generated code.

Instead, Smithy4s relies on a number of highly polymorphic interfaces to communicate with the generated code, and derive JSON codecs out of it, or turn high level user provided code into HTTP services. But in theory, the same generated code can be used conjointly with other serialisation technologies (protobuf for instance) and protocols (gRPC).

Protocol specific "hints" (called [traits](../02-the-smithy-idl/02-traits.md))can be added to the smithy models. Smithy4s accurately renders corresponding values, and allows for their retrieval via the polymorphic interfaces. This is how HTTP semantics can be derived from the generated code, for instance.

Smithy4s is also not tied to any third-party library, and users could provide integrations with existing libraries on their own side, or come up with new interesting usecases.

However, Smithy4s provides a few out-of-the-box integrations that are described in this section.