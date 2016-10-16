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
          artifacts = Seq(
            buildArtifact(
              tpe,
              new URL(artifact.url),
              downloadedArtifact.toOption,
              artifact.authentication
            ),
            // TODO: Cleaner way to get POM?
            buildArtifact(
              "pom",
              new URL(artifact.url.replaceAll(s"\\.$tpe$$", ".pom")),
              None,
              artifact.authentication
            )
          )
        )
      }
    )
  }

  def buildArtifact(`type`: String, url: URL, localFile: Option[File], auth: Option[Authentication]): NixRepoArtifact = {
    val authedUrl = auth match {
      case Some(a) => new URI(url.getProtocol, s"${a.user}:${a.password}", url.getHost, url.getPort, url.getPath, url.getQuery, url.getRef).toURL
      case None => url
    }

    NixRepoArtifact(
      `type`,
      authedUrl,
      fetchChecksum(localFile.map(_.toURI.toURL).getOrElse(authedUrl))
    )
  }

  def fetchChecksum(url: URL): String = {
    logger.info(s"Fetching $url")
    Seq("nix-prefetch-url", url.toString, "--type", "sha256").!!.trim()
  }

  private def ivyProps = Map(
    "ivy.home" -> new File(sys.props("user.home"), ".ivy2").toString
  ) ++ sys.props
}
