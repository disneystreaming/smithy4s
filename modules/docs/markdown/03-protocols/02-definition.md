---
sidebar_label: What is a Protocol?
title: What is a Protocol?
---

Most definitions of "protocol" you read online say something like, "a set of rules defined for communication on or between computers." This is a good definition, but it also highlights the issue in understanding this topic: the definition is very broad. Because of this, the word "protocol" is used to refer to different things in different contexts. In this article, we will break down what a protocol is by looking at different types and giving examples. We will then relate things back to Smithy and Smithy4s.

## TCP/IP

For starters, let's briefly address the protocols that exist at the lower layers of the internet and build from there. TCP/IP is a framework that organizes these various layers and the protocols therein. The layers are usually divided up, from lowest to highest, as follows:

#### Network Access Layer

This layer is used to refer to the hardware that exists to facilitate the connections and protocols at higher levels. In other words, this layer is concerned with the physical transmission of data from one place to another.

#### Internet Layer

This layer is mainly composed of the Internet Protocol. The Internet Protocol is what allows identifying and locating hosts on a network. The "IP" in "IPv4" and "IPv6" stands for "Internet Protocol" and those are two respective versions of that protocol.

#### Transport Layer

At this point live the TCP and UDP protocols. These protocols take two different approaches to the transferring of messages between two hosts.

#### Application Layer

Finally, at the top layer we have the protocol that we are all familiar with: http. Other examples of application-layer protocols are FTP, SSH, and SMTP. All of these protocols define their own procedures for interactions between host applications. Essentially, they are adding more structure to interactions that use TCP or UDP underneath. HTTP, for example, uses TCP to facilitate connections.

#### Summary

In summary, TCP/IP is a collection of protocols, grouped into layers, that make up the basis for interactions between computers.

## HTTP

Before we move on, let's make sure we have a common understanding of what HTTP actually is. HTTP stands for HyperText Transfer Protocol. As mentioned above, it builds upon TCP to send messages between two hosts. The most important thing for us to understand here is what HTTP defines and what it does not define.

HTTP defines semantics for communicating between host applications in a request-response model. We are all likely familiar with making an HTTP request and getting back a response. The most basic example of this is loading a web page on the internet.

Below is a collection of things that HTTP **DOES** define. Note that we are not going to be exhaustive in our definition of HTTP. The below is just to give an overview.

#### Request and Response Headers

HTTP defines that each request and response may have headers which contain additional information related to the request or response. These are key-value pairs where the key is case-insensitive.

#### Request Methods

Requests have methods such as `GET`, `PUT`, `POST`, and `DELETE` (there are currently 9 total). These methods are intended to inform the destination application what the requester wishes to do with a given resource.

#### Response Status Codes

Responses in HTTP come with an integer status code that represents the category of the result of the request. For example a status code between 200 and 299 is used to indicate that a request was successful.

#### Message Body

HTTP defines that requests and responses each have an optional message body. This is usually where the majority of information is contained.

#### Things NOT Defined in HTTP

Now that we have covered the basis of what HTTP defines, it is important to call out a few things that it does not define.

The most major thing left out of the HTTP specification is the format for data in the body of requests and responses. As far as HTTP is concerned, these bodies are just Bytes. Defining what these bodies look like is left to higher-level protocols (as we will discuss below).

HTTP also does not define what types of resources requests should be able to work with. This means requests can be used to get or modify files stored in a system, in memory state, a third party API, or anything else one could imagine.

Additionally, although HTTP provides some mechanisms for basic authentication, it does not define anything around authorization. Other protocols, such as OAuth 2.0 are often used with HTTP applications.

#### REST vs HTTP

Here is a quick note about REST since REST is often used interchangeably with HTTP despite the fact that they are two different things. REST stands for Representational State Transfer and is an architectural pattern. It is important to realize that REST is not backed by a strict RFC or other definition for what it means like HTTP is. As such, how the pattern is implemented varies throughout the industry.

RESTful systems might use HTTP, but they don't need to. Similarly, HTTP services may be RESTful, but they don't need to be. The two are related, but are not the same and one does not require the other.

## High Level Protocols

Here we will discuss several "High Level Protocols." There isn't an official term for these, but we will coin them as such to differentiate them from the protocols discussed above. All of the following protocols build on top of HTTP (although they don't have to), but define different semantics on top of it.

#### alloy#simpleRestJson

The main protocol that we have defined for use in smithy4s is `alloy#simpleRestJson`. This protocol uses RESTful HTTP semantics and has HTTP bodies that are encoded as JSON. Essentially, this protocol is building on the various protocols we defined above, and just adding some constraints around how they are used. There are more constraints around how the JSON is encoded and a few other things which you can read about in full in the [alloy documentation](https://github.com/disneystreaming/alloy#alloysimplerestjson).

#### gRPC

gRPC is a framework for remote procedure calls. At the same time, we can consider it to be another protocol that is defined at the same level that `alloy#simpleRestJson` is defined. gRPC, in its current implementation, uses HTTP version 2 under the hood. It has additional definition around what the HTTP interactions look like such as the bodies being encoded with protocol buffers (protobuf). Additionally, all gRPC messages are sent with the HTTP POST method and use a specific header to specify which remote procedure is being invoked.

There are quite a few other nuanced requirements for what makes up the gRPC protocol, but hopefully these few have showed you how gRPC builds on what HTTP provides to create a unique interaction pattern.

#### AWS Protocols

AWS themselves have defined quite a few protocols in Smithy. Examples of this range from `aws.protocols#restJson1` which is very similar to `alloy#simpleRestJson` to `aws.protocols#restXml`. You can read more about these protocols in the [Smithy documentation](https://smithy.io/2.0/aws/protocols/index.html).

## Protocols in Smithy

In the context of Smithy, a protocol is a set of concrete rules that define how data modelled in Smithy gets transcribed into lower-level semantics. A protocol definition in Smithy can be accompanied by a set of trait annotations which hold further information about how the Smithy shapes are transcribed.

For example, the `alloy#simpleRestJson` protocol relies on the `smithy.api#http` trait annotation to provide information about which HTTP endpoint will map to each Smithy operation shape. Without the `smithy.api#http` trait, one would not be able to specify the protocol-specific semantics of an API. This is how Smithy, the language itself, is protocol agnostic while still allowing users to define APIs which conform to various protocols.

#### How to Define a Protocol

Protocols can be defined in Smithy as traits that are marked with the `@protocolDefinition` [trait](https://smithy.io/2.0/spec/protocol-traits.html#protocoldefinition-trait). For example, the (slightly simplified) definition of `alloy#simpleRestJson` looks like:

```smithy
namespace alloy

@protocolDefinition()
@trait(selector: "service")
structure simpleRestJson {}
```

This then allows you to annotate service shapes with the protocol. Although the protocol is defined in Smithy like this, the exact meaning of the protocol is defined elsewhere in documentation and more importantly in interpreters that implement the protocol (such as the interpreters that smithy4s uses to create http4s applications). Additionally, protocol definitions typically come with test cases, also [defined in Smithy](https://smithy.io/2.0/additional-specs/http-protocol-compliance-tests.html), which help to define the specific semantics of the protocol.

## Conclusion

Hopefully, this article has helped to outline what a protocol is and more specifically what layers of protocols we are building upon to create something like the `alloy#simpleRestJson` protocol. If you have any more questions about protocols, especially the protocols we have defined and use in Smithy4s, don't hesitate to reach out.
