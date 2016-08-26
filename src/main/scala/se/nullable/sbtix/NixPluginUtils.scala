package se.nullable.sbtix

object NixPluginUtils {
  def quote(s: String) = "\"" + s + "\""

  def indent(str: String) = {
    val spaces = " " * 2
    str.replace("\n", "\n" + spaces)
  }
}
