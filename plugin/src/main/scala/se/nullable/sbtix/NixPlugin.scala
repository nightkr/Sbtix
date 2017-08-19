package se.nullable.sbtix

import coursier.CoursierPlugin
import sbt.Keys._
import sbt._

object NixPlugin extends AutoPlugin {

  lazy val genNixProjectTask =
    Def.task {
      // use all resolvers except the projectResolver and local ivy/maven file Resolvers
      val exceptResolvers = Set(projectResolver.value, Resolver.mavenLocal, Resolver.defaultLocal)
      val genNixResolvers = (fullResolvers.value ++ externalResolvers.value).toSet -- exceptResolvers

      val logger = sLog.value

      val modules = (allDependencies.value.toSet
        .filterNot(_.extraAttributes.contains("e:nix"))
        -- projectDependencies.value)

      val depends = modules.flatMap(coursier.FromSbt.dependencies(_, scalaVersion.value, scalaBinaryVersion.value, "jar")).map(_._2)
        .filterNot {
          _.module.organization == "se.nullable.sbtix"
        } //ignore the sbtix dependency that gets added because of the global sbtix plugin

      GenProjectData(scalaVersion.value, sbtVersion.value, depends, genNixResolvers, CoursierPlugin.autoImport.coursierCredentials.value.toSet)
    }

  import autoImport._
  lazy val genNixCommand =
    Command.command("genNix") { initState =>
      val extracted = Project.extract(initState)
      val repoFile = extracted.get(nixRepoFile)
      var state = initState

      val genProjectDataSet = (for {
        project <- extracted.structure.allProjectRefs
        genProjectData <- Project.runTask(genNixProject in project, state) match {
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
      } yield genProjectData).toSet

      //val (dependencySeqSet, resolverSeqSeq,credentialsSeq) = genProjectDataSeq

      val dependencies = genProjectDataSet.flatMap(_.dependencies)
      val resolvers = genProjectDataSet.flatMap(_.resolvers)
      val credentials = Map(genProjectDataSet.flatMap(_.credentials).toSeq: _*)
      val versioning = genProjectDataSet.map(x => (x.scalaVersion, x.sbtVersion))


      val fetcher = new CoursierArtifactFetcher(state.log, resolvers, credentials)
      val (repos, artifacts, errors) = fetcher(dependencies)

      val flatErrors = errors.flatMap(_.errors)

      if (flatErrors.size > 0) {
        state.log.error("\n\nSbtix Resolution Errors:\n")
        flatErrors.foreach(e => state.log.error(s"${e.toString()}\n"))
      }

      if (!extracted.get(manualRepoFile).exists) IO.write(
        extracted.get(manualRepoFile),
        resource2string("/manual-repo.nix")
      )

      IO.write(repoFile, NixWriter(versioning, repos, artifacts))
      state
    }

  lazy val genCompositionCommand =
    Command.command("genComposition") { state =>
      val proj = Project.extract(state)
      val cmpFile = proj.get(compositionFile)
      val t = proj.get(compositionType)

      if (t == "project") {
        state.log.warn("Composition type `project` is internal and should be avoided!")
      }

      // generation behavior is optional.
      // `cmpFile.exists` needs to be triggered as the file is generated once and should be editable
      // by the developer
      if (proj.get(generateComposition)) {
        if (!cmpFile.exists) IO.write(
          cmpFile,
          CompositionWriter(t, proj.currentProject.id)
        )
        IO.write(
          proj.get(sbtix),
          resource2string("/sbtix.nix")
        )
      }

      state
    }

  override def requires: Plugins = CoursierPlugin

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings = Seq(
    nixRepoFile := baseDirectory.value / "repo.nix",
    manualRepoFile := baseDirectory.value / "manual-repo.nix",

    compositionFile := baseDirectory.value / "default.nix",
    generateComposition := true,
    compositionType := "program",
    sbtix := baseDirectory.value / "sbtix.nix",

    genNixProject := genNixProjectTask.value,

    commands ++= Seq(
      genNixCommand,
      genCompositionCommand
    )
  )

  case class GenProjectData(scalaVersion: String, sbtVersion: String, dependencies: Set[coursier.Dependency], resolvers: Set[Resolver], credentials: Set[(String, coursier.Credentials)])

  object autoImport {
    val nixRepoFile = settingKey[File]("the path to put the nix repo definition in")
    val genNixProject = taskKey[GenProjectData]("generate a Nix definition for building the maven repo")
    val manualRepoFile = settingKey[File]("path to `manual-repo.nix`")

    // parameters for composition file
    val compositionFile = settingKey[File]("path to the file which contains the composition")
    val generateComposition = settingKey[Boolean]("Whether or not to generate a composition")
    val compositionType = settingKey[String]("project type to be built by SBTix (`program`, `library` or `project`)")
    val sbtix = settingKey[File]("path for sbtix.nix file")
  }

}
