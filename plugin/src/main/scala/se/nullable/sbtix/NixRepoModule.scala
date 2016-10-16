package se.nullable.sbtix

import sbt._
import se.nullable.sbtix.NixPluginUtils._

case class NixRepoModule(module: ModuleID, artifacts: Seq[NixRepoArtifact]) {
  def toNixDef = s"$toNixRef = $toNixValue;"

  def toNixRef = "" +
    s"${quote(module.organization)}" +
    s".${quote(module.name)}" +
    s".${quote(module.revision)}"

  def toNixValue =
    s"""{
        |  ${indent(artifacts.map(_.toNix).mkString("\r\n"))}
        |}""".stripMargin
}

