package se.nullable.sbtix

import java.io.{File, FileOutputStream, IOException}
import java.net.URL
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.Semaphore

import sbt.Logger
import sun.misc.IOUtils

import scala.util.{Failure, Success, Try}

object FindArtifactsOfRepo {

  def fetchChecksum(originalUrl: String, artifactType: String, url: URL): Try[String] =
    calculateChecksum(url)

  private def calculateChecksum(url: URL): Try[String] = {

    try {
      val hash = MessageDigest getInstance "SHA-256"
      val is = url.openConnection.getInputStream()
      val input = IOUtils.readFully(is, -1, true)
      is.close()
      Success {
        hash.digest(input) map { "%02X" format _ } mkString
      }
    } catch { case e: IOException =>
        Failure(e)
    }
  }
}

class FindArtifactsOfRepo(repoName: String, root: String) {

  def findArtifacts(logger: Logger, modules: Set[GenericModule]): Set[NixArtifact] = modules.flatMap { ga =>
    val rootUrl = new URL(root)

    val authedRootURI = ga.authed(rootUrl) //authenticated version of the rootUrl

    val allArtifacts = recursiveListFiles(ga.localSearchLocation)
    //get list of files at location
    val targetArtifacts = allArtifacts.filter(f => """.*(\.jar|\.pom|ivy.xml)$""".r.findFirstIn(f.getName).isDefined) //filter for interesting files

    targetArtifacts.map { artifactLocalFile =>

      val calcUrl = ga.calculateURI(artifactLocalFile).toURL

      NixArtifact(
        repoName,
        calcUrl.toString.replace(authedRootURI.toString, "").stripPrefix("/"),
        calcUrl.toString,
        FindArtifactsOfRepo.fetchChecksum(calcUrl.toString, "Artifact", artifactLocalFile.toURI.toURL).get)
    }
  }

  def findMetaArtifacts(logger: Logger, metaArtifacts: Set[MetaArtifact]): Set[NixArtifact] = {
    metaArtifacts.map { meta =>
      NixArtifact(
        repoName,
        meta.artifactUrl.replace(root, "").stripPrefix("/"),
        meta.artifactUrl,
        meta.checkSum
      )
    }
  }

}

