package se.nullable.sbtix

import sbt._
import sbt.ProjectRef
import sbt.{ Logger, ModuleID, Resolver, PatternsBasedRepository }

object NixResolver {

  def resolve(resolver: Resolver) = {
    val nixResolver = resolver match {
      case ivy: PatternsBasedRepository =>
        val pat = ivy.patterns.artifactPatterns.head
        val endIndex = pat.indexOf("[")
        val root = pat.substring(0, endIndex)
        val pattern = pat.substring(endIndex)
        NixResolver(ivy.name, root, Some(pattern))
      case mvn: MavenRepository => NixResolver(mvn.name, mvn.root, None)
      case cr: ChainedResolver => ???
      case jn1: JavaNet1Repository => ???
      case raw: RawRepository => ???
    }

    nixResolver
  }
}

case class NixResolver(private val name: String, rootUrl: String, pattern: Option[String]) {
  private val repoName = "nix-" + name

  private def isMatch(urlString: String) = urlString.startsWith(rootUrl)
  private val nixRepo = NixRepo(repoName, pattern)
  private val finder = new FindArtifactsOfRepo(repoName, rootUrl)

  def filterArtifacts(logger: Logger, modules: Seq[GenericModule]): Option[(NixRepo, Seq[NixArtifact])] = {
    modules.filter(m => isMatch(m.url.toString)) match {
      case Nil => None
      case mods => Some((nixRepo, finder.findArtifacts(logger, mods)))
    }
  }

  def filterMetaArtifacts(logger: Logger, metaArtifacts: Seq[MetaArtifact]): Option[(NixRepo, Seq[NixArtifact])] = {
    metaArtifacts.filter(m => isMatch(m.artifactUrl)) match {
      case Nil => None
      case metas => Some((nixRepo, finder.findMetaArtifacts(logger, metas)))
    }
  }
}