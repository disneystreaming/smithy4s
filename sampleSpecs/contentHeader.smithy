$version: "2"

namespace smithy4s.example.content

use smithy.api#mediaType
use smithy.api#http

/// Service to test Content-Type header behavior
use alloy#simpleRestJson

@simpleRestJson
service ContentHeaderTestService {
    operations: [
        DefaultContentHeader,
        XmlInput,
        XmlInputJsonOutput,
        BlobInputWithMediaType,
        BlobInputNoMediaType,
        NoBodyOperation
    ]
}

/// JSON payload type
@mediaType("application/json")
string JsonPayload

/// XML payload type
@mediaType("application/xml")
string XmlPayload

/// Plain text payload type
@mediaType("text/plain")
string PlainTextPayload

/// PNG image blob type
@mediaType("image/png")
blob PngImage

/// Operation with no media types - should use default Content-Type header
@http(method: "POST", uri: "/default")
operation DefaultContentHeader {
    input := {
        @httpPayload
        @required
        data: String
    }
    output := {
        @httpPayload
        result: String
    }
}

/// Operation with XML input media type
@http(method: "POST", uri: "/xml-input")
operation XmlInput {
    input := {
        @httpPayload
        @required
        data: XmlPayload
    }
    output := {
        @httpPayload
        result: String
    }
}

/// Operation with different media types for input and output
@http(method: "POST", uri: "/xml-json")
operation XmlInputJsonOutput {
    input := {
        @httpPayload
        @required
        data: XmlPayload
    }
    output := {
        @httpPayload
        result: JsonPayload
    }
}

/// Operation with Blob input that has media type
@http(method: "POST", uri: "/blob-with-media")
operation BlobInputWithMediaType {
    input := {
        @httpPayload
        @required
        image: PngImage
    }
    output := {
        @httpPayload
        data: String
    }
}

/// Operation with Blob input without media type
@http(method: "POST", uri: "/blob-no-media")
operation BlobInputNoMediaType {
    input := {
        @httpPayload
        @required
        image: Blob
    }
    output := {
        @httpPayload
        data: String
    }
}

/// Operation with only metadata (no body) - should not have Content-Type header
@http(method: "GET", uri: "/no-body")
operation NoBodyOperation {
    input := {
        @httpQuery("q")
        query: String
    }
    output := {
        @httpPayload
        data: String
    }
}

