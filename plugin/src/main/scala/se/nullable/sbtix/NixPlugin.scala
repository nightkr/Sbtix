package se.nullable.sbtix

import coursier.CoursierPlugin
import sbt.Keys._
import sbt._

object NixPlugin extends AutoPlugin {

  import autoImport._

  lazy val genNixProjectTask =
    Def.task {
      // use all resolvers except the projectResolver and local ivy/maven file Resolvers
      val exceptResolvers = Seq(projectResolver.value, Resolver.mavenLocal, Resolver.defaultLocal)
      val genNixResolvers = (fullResolvers.value ++ externalResolvers.value).distinct.diff(exceptResolvers)
      
      val logger = sLog.value

      val modules = allDependencies.value.toSet -- projectDependencies.value
      
     
      val depends = modules.flatMap(coursier.FromSbt.dependencies(_, scalaVersion.value, scalaBinaryVersion.value, "jar")).map(_._2)
                           .filterNot { _.module.organization == "se.nullable.sbtix" }  //ignore the sbtix dependency that gets added because of the global sbtix plugin

      (depends,genNixResolvers, CoursierPlugin.autoImport.coursierCredentials.value)
    }

  lazy val genNixCommand =
    Command.command("genNix") { initState =>
      val extracted = Project.extract(initState)
      val repoFile = extracted.get(nixRepoFile)
      var state = initState

      val dependencyResolverTupleCollection = (for {
        project <- extracted.structure.allProjectRefs
        dependencyResolverTuple <- Project.runTask(genNixProject in project, state) match {
          case Some((_state, Value(taskOutput))) =>
            state = _state
            Some(taskOutput)
          case Some((_state, Inc(inc: Incomplete))) =>
            state = _state
            state.log.error(s"genNixProject task did not complete $inc for project $project")
            None
          case None =>
            state.log.warn(s"NixPlugin not enabled for project $project, skipping...")
            None
        }
      } yield dependencyResolverTuple)

      val (dependencySeqSet, resolverSeqSeq,credentialsSeq) = dependencyResolverTupleCollection.unzip3

      val dependencies = dependencySeqSet.flatten.distinct
      val resolvers = resolverSeqSeq.flatten.distinct
      val credentials = Map(credentialsSeq.flatten.distinct :_*)

      val fetcher = new CoursierArtifactFetcher(state.log, resolvers, credentials)
      val (repos,artifacts,errors) =  fetcher(dependencies)
      
      val flatErrors = errors.flatMap(_.errors)
      
      if (flatErrors.size > 0) { 
        state.log.error("\n\nSbtix Resolution Errors:\n")
        flatErrors.foreach( e => state.log.error(s"${e.toString()}\n"))
      }
      
      IO.write(repoFile, NixWriter(repos, artifacts))
      state
    }

  override def requires: Plugins = CoursierPlugin

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings = Seq(
    nixRepoFile := baseDirectory.value / "repo.nix",
    genNixProject := genNixProjectTask.value,
    commands += genNixCommand)

  object autoImport {
    val nixRepoFile = settingKey[File]("the path to put the nix repo definition in")
    val genNixProject = taskKey[(Set[coursier.Dependency], Seq[Resolver], Map[String,coursier.Credentials])]("generate a Nix definition for building the maven repo")
  }

}
