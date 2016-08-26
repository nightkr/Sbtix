package se.nullable.sbtix

import se.nullable.sbtix.NixPluginUtils.indent

case class NixRepo(artifacts: Seq[NixRepoModule]) {
  def toNix: String =
    s"""{
        |  ${indent(artifacts.map(_.toNixDef).mkString("\r\n"))}
        |}""".stripMargin
}