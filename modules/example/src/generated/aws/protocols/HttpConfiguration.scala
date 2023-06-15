package aws.protocols


/** Contains HTTP protocol configuration for HTTP-based protocols.
  * @param http
  *   The priority ordered list of supported HTTP protocol versions.
  * @param eventStreamHttp
  *   The priority ordered list of supported HTTP protocol versions that
  *   are required when using event streams with the service. If not set,
  *   this value defaults to the value of the `http` member. Any entry in
  *   `eventStreamHttp` MUST also appear in `http`.
  */
trait HttpConfiguration {
  def http: Option[List[String]]
  def eventStreamHttp: Option[List[String]]
}
