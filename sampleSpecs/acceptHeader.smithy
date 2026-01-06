$version: "2"

namespace smithy4s.example.accept

use smithy.api#mediaType
use smithy.api#http

/// Service to test Accept header behavior
use alloy#simpleRestJson

@simpleRestJson
service AcceptHeaderTestService {
    operations: [
        DefaultAcceptHeader,
        XmlOutput,
        JsonInputXmlOutput,
        BlobOutputWithMediaType,
        BlobOutputNoMediaType
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

/// Operation with no media types - should use default Accept header
@http(method: "POST", uri: "/default")
operation DefaultAcceptHeader {
    input := {
        data: String
    }
    output := {
        @httpPayload
        result: String
    }
}

/// Operation with XML output media type
@http(method: "POST", uri: "/xml-output")
operation XmlOutput {
    input := {
        @httpPayload
        data: String
    }
    output := {
        @httpPayload
        result: XmlPayload
    }
}

/// Operation with different media types for input and output
@http(method: "POST", uri: "/json-xml")
operation JsonInputXmlOutput {
    input := {
        @httpPayload
        data: JsonPayload
    }
    output := {
        @httpPayload
        result: XmlPayload
    }
}

/// Operation with Blob output that has media type
@http(method: "POST", uri: "/blob-with-media")
operation BlobOutputWithMediaType {
    input := {
        @httpPayload
        data: String
    }
    output := {
        @httpPayload
        image: PngImage
    }
}

/// Operation with Blob output without media type
@http(method: "POST", uri: "/blob-no-media")
operation BlobOutputNoMediaType {
    input := {
        @httpPayload
        data: String
    }
    output := {
        @httpPayload
        image: Blob
    }
}


