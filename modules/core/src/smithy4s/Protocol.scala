package smithy4s

trait Protocol[A] {
  def hintMask: HintMask
}
