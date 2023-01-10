package smithy4s

trait ServiceFunctions[Alg[_[_, _, _, _, _]], Functions[_[_, _, _, _, _]]]
    extends Service[Alg] {
  def reifiedEndpoints: Functions[Endpoint]
}
