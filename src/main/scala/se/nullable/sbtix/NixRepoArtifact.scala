package se.nullable.sbtix

import java.net.URL

import se.nullable.sbtix.NixPluginUtils._

case class NixRepoArtifact(`type`: String, url: URL, sha256: String) {
  def toNix =
    s"""${quote(`type`)} = {
       |  url = ${quote(url.toString)};
       |  sha256 = ${quote(sha256)}";
       |};""".stripMargin
}
