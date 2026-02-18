package smithy4s.codegen.internals

import smithy4s.codegen.internals.LineSyntax.LineInterpolator

object RenderStringLiteral {
  def render(raw: String): Line = {
    val str = raw
      // Replace sequences like "\\uD83D" (how Smithy specs refer to unicode characters)
      // with unicode character escapes like "\uD83D" that can be parsed in the regex implementations on all platforms.
      // See https://github.com/disneystreaming/smithy4s/pull/499
      .replace("\\\\u", "\\u")
    // If the string contains "$", the use of -Xlint:missing-interpolator when
    // compiling the generated code will result in warnings. To prevent that we
    // render any such strings as interpolated strings (even though that would
    // otherwise be unecessary) so that we can render "$" as "$$", which get
    // converted back to "$" during interpolation.
    val escaped = if (str.contains('$')) s"s${str.replace("$", "$$")}" else str

    line"$escaped"
  }
}
