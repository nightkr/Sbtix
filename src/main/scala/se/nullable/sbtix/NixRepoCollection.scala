package se.nullable.sbtix

import sbt.ProjectRef
import se.nullable.sbtix.NixPluginUtils._

case class NixRepoCollection(repos: Map[ProjectRef, NixRepo]) {
  def toNix =
    s"""{
        |  ${indent(repos.map(projectToNix).mkString("\r\n"))}
        |}
        |""".stripMargin

  def projectToNix: PartialFunction[(ProjectRef, NixRepo), String] = {
    case (project, repo) =>
      s"${quote(project.project)} = ${repo.toNix};"
  }
}
