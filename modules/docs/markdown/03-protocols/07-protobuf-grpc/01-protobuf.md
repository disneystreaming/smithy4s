---
sidebar_label: Protobuf
title: Protobuf
---

For convenience, the smithy4s build plugins generate protobuf (`.proto`) definitions translated from smithy specifications out of the box. However, this translation is limited to the transitive closure of shapes that have the `alloy.proto#protoEnabled` or the `alloy.proto#grpc` traits.

The location of these protobuf specifications is driven by the `smithy4sResourceDir` setting in SBT and the `smithy4sResourceOutputDir` in mill.

The semantics of the smithy to protobuf translation are following the [alloy specification](https://github.com/disneystreaming/alloy/blob/main/docs/serialisation/protobuf.md).
