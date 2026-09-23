$version: "2"

namespace smithy4s.example.aws

@aws.api#service(sdkId: "MyThing", endpointPrefix: "mything")
service MyAwsService {}

/// Mirrors the shape of AWS SES v2: the endpoint prefix ("email"), the ARN
/// namespace ("ses") and the sigv4 signing name ("ses") are all different from
/// each other, so the host and the credential scope must be resolved
/// independently.
@aws.auth#sigv4(name: "ses")
@aws.protocols#awsJson1_0
@aws.api#service(
    sdkId: "PrefixDiffersFromSigningName"
    arnNamespace: "ses"
    endpointPrefix: "email"
)
service PrefixDiffersFromSigningName {
    operations: [DoThing]
}

/// Mirrors the shape of AWS SageMaker (issue #1568): the endpoint prefix is
/// dotted and differs from the signing name, which matches the ARN namespace.
@aws.auth#sigv4(name: "sagemaker")
@aws.protocols#awsJson1_1
@aws.api#service(
    sdkId: "DottedPrefix"
    arnNamespace: "sagemaker"
    endpointPrefix: "api.sagemaker"
)
service DottedPrefix {
    operations: [DoThing]
}

/// Mirrors the shape of the AWS Account API (issue #1532): no endpoint prefix
/// at all, so both the host and the signing name fall back to the ARN
/// namespace. Before the fix, the host was derived from the *operation* name.
@aws.auth#sigv4(name: "account")
@aws.protocols#awsJson1_0
@aws.api#service(sdkId: "NoEndpointPrefix", arnNamespace: "account")
service NoEndpointPrefix {
    operations: [DoThing]
}

/// A service with no sigv4 trait, to check that the signing name falls back to
/// the ARN namespace rather than to the endpoint prefix.
@aws.protocols#awsJson1_0
@aws.api#service(
    sdkId: "NoSigv4"
    arnNamespace: "arnnamespace"
    endpointPrefix: "endpointprefix"
)
service NoSigv4 {
    operations: [DoThing]
}

operation DoThing {
    input := {}
    output := {}
}
