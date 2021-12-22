namespace smithy4s.example

structure StringBody {
  @httpPayload
  @required
  str: String
}

structure BlobBody {
  @httpPayload
  @required
  blob: Blob
}

structure CSVBody {
  @httpPayload
  @required
  csv: CSV
}

structure PNGBody {
  @httpPayload
  @required
  png: PNG
}

@mediaType("text/csv")
string CSV

@mediaType("image/png")
blob PNG
