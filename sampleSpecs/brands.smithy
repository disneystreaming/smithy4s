$version: "2"

namespace smithy4s.example

use smithy4s.example.common#BrandList

service BrandService {
  version: "1",
  operations: [AddBrands]
}

@http(method: "POST", uri: "/brands", code: 200)
operation AddBrands {
  input: AddBrandsInput
}

structure AddBrandsInput {
  brands: BrandList
}
