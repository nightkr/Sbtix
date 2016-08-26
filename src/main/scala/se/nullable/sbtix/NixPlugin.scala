package se.nullable.sbtix

import coursier.CoursierPlugin
import sbt.Keys._
import sbt._

object NixPlugin extends AutoPlugin {

  import autoImport._

  lazy val genNixRepoTask =
    Def.task {
      val fetcher = new CoursierArtifactFetcher(
        scalaVersion.value,
        scalaBinaryVersion.value,
        sLog.value
      )

      val sVersion = scalaVersion.value
      val isDotty = ScalaInstance.isDotty(sVersion)
      fetcher.buildNixRepo(allDependencies.value.toSet -- projectDependencies.value, externalResolvers.value)
    }
  lazy val genNixCommand =
    Command.command("genNix") { initState =>
      val extracted = Project.extract(initState)
      val repoFile = extracted.get(nixRepoFile)
      var state = initState

      val repos = NixRepoCollection((for {
        project <- extracted.structure.allProjectRefs
        file <- Project.runTask(genNixRepo in project, state) match {
          case Some((_state, Value(file))) =>
            state = _state
            Some(file)
          case None =>
            state.log.warn(s"NixPlugin not enabled for project $project, skipping...")
            None
        }
      } yield (project, file)).toMap)

      IO.write(repoFile, repos.toNix)
      state
    }

  override def requires: Plugins = CoursierPlugin

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings = Seq(
    nixRepoFile := baseDirectory.value / "repo.nix",
    genNixRepo := genNixRepoTask.value,
    commands += genNixCommand
  )

  object autoImport {
    val nixRepoFile = settingKey[File]("the path to put the nix repo definition in")
    val genNixRepo = taskKey[NixRepo]("generate a Nix definition for building the maven repo")
  }

}
