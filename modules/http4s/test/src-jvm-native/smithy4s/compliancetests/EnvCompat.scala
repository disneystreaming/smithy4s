package smithy4s.compliancetests

trait EnvCompat {
  def env: Map[String, String] = sys.env

}
