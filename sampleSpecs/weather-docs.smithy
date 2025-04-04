$version: "2"

namespace weather

use alloy#nullable
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

structure Person1 {
    item: String = null
}

structure Person2 {
    item: String
}

structure Person3 {
    item: String = ""
}

structure Person4 {
    @nullable
    item: String = ""
}

structure Person5 {
    @nullable
    item: String = null
}
