$version: "2"

namespace weather

use alloy#simpleRestJson

@simpleRestJson
service WeatherService {
    operations: [GetWeather]
}

@http(method: "GET", uri: "/weather/{city}")
operation GetWeather {
    input := {
        @httpLabel
        @required
        city: String
    }
    output := {
        @required
        weather: String
    }
}

structure Dog {
    @required
    name: String
}
