$version: "2"

namespace smithy4s.sandbox.oauth

@tokenExchange
service TokenApi {
    operations: [
        CreateAccessToken
    ]
    errors: [
        BadRequest
    ]
}

@http(method: "POST", uri: "/token")
operation CreateAccessToken {
    input := {
        @required
        @xmlName("client_id")
        clientId: ClientId
        @required
        @xmlName("client_secret")
        clientSecret: ClientSecret
        @required
        @xmlName("grant_type")
        grantType: GrantType
        @required
        @xmlName("refresh_token")
        refreshToken: RefreshToken
    }
    output := {
        @required
        @jsonName("access_token")
        accessToken: AccessToken
        @required
        @jsonName("expires_in")
        expiresIn: ExpiresIn
        @required
        @jsonName("refresh_token")
        refreshToken: RefreshToken
        @required
        @jsonName("token_type")
        tokenType: TokenType
    }
}

enum GrantType {
    REFRESH_TOKEN = "refresh_token"
}

string ClientId

string ClientSecret

long ExpiresIn

string AccessToken

string RefreshToken

enum TokenType {
    BEARER = "Bearer"
}

@error("client")
@httpError(400)
structure BadRequest {
    @required
    error: Error
}

enum Error {
    INVALID_CLIENT = "invalid_client"
    INVALID_GRANT = "invalid_grant"
    UNAUTHORIZED_CLIENT = "unauthorized_client"
    UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type"
}
