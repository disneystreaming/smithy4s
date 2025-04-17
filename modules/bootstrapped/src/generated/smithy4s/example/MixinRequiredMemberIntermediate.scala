package smithy4s.example


trait MixinRequiredMemberIntermediate extends MixinRequiredMember {
  def description: String
  def extraField: Option[String]
}
