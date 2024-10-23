val routes: Resource[IO, HttpRoutes[IO]] =
  SimpleRestJsonBuilder.routes(AdminServiceImpl).resource
