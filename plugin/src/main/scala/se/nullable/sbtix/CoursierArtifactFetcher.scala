package se.nullable.sbtix

import java.io.File
import java.net.{URI, URL}

import sbt.ProjectRef
import coursier._
import coursier.core.Authentication
import sbt.{Logger, ModuleID, Resolver, PatternsBasedRepository}

import scala.sys.process._

case class GenericModule(artifact: Artifact, dep:Dependency, localFile:java.io.File) {
  val isIvy = localFile.getParentFile().getName() == "jars"
  val moduleId = ToSbt.moduleId(dep)
  val url = new URL(artifact.url)
}

class CoursierArtifactFetcher(scalaVersion: String,
                              scalaBinaryVersion: String,
                              logger: Logger
                             ) {
  def buildNixProject(projectRef:ProjectRef,modules: Set[ModuleID], resolvers: Seq[Resolver], credentials: Map[String, Credentials]): (Seq[se.nullable.sbtix.GenericModule], Seq[sbt.Resolver])
= {
    val initResolution = Resolution(
      modules
        .flatMap(FromSbt.dependencies(_, scalaVersion, scalaBinaryVersion, "jar"))
        .map(_._2)
    )
 
    val repos = resolvers.flatMap{resolver => 
      FromSbt.repository(resolver, ivyProps, logger, credentials.get(resolver.name).map(_.authentication))}
    val fetch = Fetch.from(repos, Cache.fetch())
    val resolution = initResolution.process.run(fetch).unsafePerformSync
    assert(resolution.errors.isEmpty, resolution.errors)

    val genericModules = resolution.dependencyArtifacts.flatMap { case ((dependency, artifact)) =>
        val downloadedArtifact = Cache.file(artifact).run.unsafePerformSync
        
        downloadedArtifact.toOption.map{localFile=> GenericModule(artifact,dependency,localFile)}
    }

   (genericModules,resolvers)
  }
 

  private def ivyProps = Map(
    "ivy.home" -> new File(sys.props("user.home"), ".ivy2").toString
  ) ++ sys.props
}
