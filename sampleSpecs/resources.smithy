$version: "2"

namespace smithy4s.example

service Library {
  resources: [Publisher]
}

resource Publisher {
  resources: [Book]
  read: ListPublishers
}

resource Book {
  read: GetBook
  operations: [BuyBook]
}

@readonly
operation ListPublishers {
  input: Unit
  output := {
    @required
    publishers: PublishersList
  }
}

@readonly
operation GetBook {
}

operation BuyBook {
}

list PublishersList {
  member: PublisherId
}

string PublisherId
