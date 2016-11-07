package se.nullable.sbtix

import coursier.CoursierPlugin
import sbt.Keys._
import sbt._

object NixPlugin extends AutoPlugin {

  import autoImport._

  lazy val genNixProjectTask =
    Def.task {
      val fetcher = new CoursierArtifactFetcher(sLog.value)

      val sVersion = scalaVersion.value
      val isDotty = ScalaInstance.isDotty(sVersion)

      val modules = allDependencies.value.toSet -- projectDependencies.value
      val deps = modules.flatMap(coursier.FromSbt.dependencies(_, scalaVersion.value, scalaBinaryVersion.value, "jar")).map(_._2)

      //coursier must take dependencies one at a time, otherwise it only resolves the most recent version of a module, which causes missed dependencies.
      val (a,b) = deps.toSeq.map(fetcher.buildNixProject(externalResolvers.value, CoursierPlugin.autoImport.coursierCredentials.value)).unzip
      (a.flatten,b.flatten)
    }
  lazy val genNixCommand =
    Command.command("genNix") { initState =>
      val extracted = Project.extract(initState)
      val repoFile = extracted.get(nixRepoFile)
      var state = initState

      val moduleResolversTupleCollection = (for {
        project <- extracted.structure.allProjectRefs
        modulesResolversTuple <- Project.runTask(genNixProject in project, state) match {
          case Some((_state, Value(taskOutput))) =>
            state = _state
            Some(taskOutput)
          case Some((_state, Inc(inc:Incomplete))) => 
            state = _state
            state.log.error(s"genNixProject task did not complete $inc for project $project")
            None
          case None =>
            state.log.warn(s"NixPlugin not enabled for project $project, skipping...")
            None
        }
      } yield modulesResolversTuple)

      val (modulesSeqSeq,resolversSeqSeq) = moduleResolversTupleCollection.unzip

      val modules = modulesSeqSeq.flatten.distinct
      val resolvers = resolversSeqSeq.flatten.distinct

      IO.write(repoFile, NixWriter(state.log, modules, resolvers))
      state
    }

  override def requires: Plugins = CoursierPlugin

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings = Seq(
    nixRepoFile := baseDirectory.value / "repo.nix",
    genNixProject:= genNixProjectTask.value,
    commands += genNixCommand
  )

  object autoImport {
    val nixRepoFile = settingKey[File]("the path to put the nix repo definition in")
    val genNixProject = taskKey[(Seq[se.nullable.sbtix.GenericModule], Seq[sbt.Resolver])]("generate a Nix definition for building the maven repo")
  }

}
