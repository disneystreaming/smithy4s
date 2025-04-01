object AdminServiceImpl extends AdminService[IO] {
  def getUser(id: String): IO[User] = ???
}
