package se.nullable.sbtix

import java.io.File
import java.net.{URI, URL}

import coursier._
import coursier.core.Authentication
import sbt.{Logger, ModuleID, Resolver}

import scala.sys.process._

class CoursierArtifactFetcher(scalaVersion: String,
                              scalaBinaryVersion: String,
                              logger: Logger
                             ) {
  def buildNixRepo(modules: Set[ModuleID], resolvers: Seq[Resolver], credentials: Map[String, Credentials]): NixRepo = {
    val initResolution = Resolution(
      modules
        .flatMap(FromSbt.dependencies(_, scalaVersion, scalaBinaryVersion, "jar"))
        .map(_._2)
    )
    val repos = resolvers.flatMap(resolver => FromSbt.repository(resolver, ivyProps, logger, credentials.get(resolver.name).map(_.authentication)))
    val fetch = Fetch.from(repos, Cache.fetch())
    val resolution = initResolution.process.run(fetch).run
    assert(resolution.errors.isEmpty, resolution.errors)

    NixRepo(
      resolution.dependencyArtifacts.map { case ((dependency, artifact)) =>
        val downloadedArtifact = Cache.file(artifact).run.run
        val tpe = artifact.attributes.`type`

        NixRepoModule(
          module = ToSbt.moduleId(dependency),
          artifacts = 
            downloadedArtifact.toOption.map(d=>findArtifacts(new URL(artifact.url),d,  artifact.authentication)).toSeq.flatten
        )
      }
    )
  } 

  def findArtifacts(url: URL, localFile: File, auth: Option[Authentication]): Seq[NixRepoArtifact] = {

    val authedUri = auth match {
          case Some(a) => new URI(url.getProtocol, s"${a.user}:${a.password}", url.getHost, url.getPort, url.getPath, url.getQuery, url.getRef)
          case None => url.toURI
    }

    val isIvy = localFile.getParentFile().getName() == "jars"

    val localSearchLocation = if (isIvy) {
       localFile.getParentFile().getParentFile()
    } else {
       localFile.getParentFile()
    }

    def parentURI(uri:URI) = if (uri.getPath().endsWith("/")) uri.resolve("..") else uri.resolve(".")

    val calculatedParentURI = if (isIvy) {
      parentURI(parentURI(authedUri))
    } else {
      parentURI(authedUri)
    }

    def calculateURI(f:File) = if (isIvy) {
      calculatedParentURI.resolve(f.getParentFile().getName() + "/" + f.getName())
    } else {
      calculatedParentURI.resolve(f.getName())
    }
                 
    def recursiveListFiles(f: File): Array[File] = {
       val these = f.listFiles
          these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
    }

    val allArtifacts = recursiveListFiles(localSearchLocation)
    val targetArtifacts = allArtifacts.filter(f => """.*(\.jar|\.pom|ivy.xml)$""".r.findFirstIn(f.getName).isDefined)

    def getFileType(file:File) : String = {
       val name = file.getName()
       if (name == "ivy.xml") return "ivy"
             
       name.substring(name.lastIndexOf(".") + 1);
    }

    targetArtifacts.map{ artifactLocalFile =>

      val calcUrl = calculateURI(artifactLocalFile).toURL

      NixRepoArtifact(
       getFileType(artifactLocalFile),
       calcUrl,
       fetchChecksum(artifactLocalFile.toURI.toURL)
    )}.toSeq
  }

  def fetchChecksum(url: URL): String = {
    logger.info(s"Fetching $url")
    Seq("nix-prefetch-url", url.toString, "--type", "sha256").!!.trim()
  }

  private def ivyProps = Map(
    "ivy.home" -> new File(sys.props("user.home"), ".ivy2").toString
  ) ++ sys.props
}
