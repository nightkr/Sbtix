package se.nullable.sbtix

import java.io.{File, FileOutputStream, IOException}
import java.net.URL
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.Semaphore

import sbt.Logger

import scala.util.{Failure, Success, Try}

object FindArtifactsOfRepo {
  private val semaphore = new Semaphore(4, false)

  def fetchChecksum(originalUrl: String, artifactType: String, url: URL): Try[String] = {
    semaphore.acquireUninterruptibly()
    val checksum = calculateChecksum(url)
    semaphore.release()

    checksum
  }

  private def calculateChecksum(url: URL): Try[String] = {
    val tmpFile = File.createTempFile(s"sbtix-${url.toString}", ".tmp")
    val path = tmpFile.toPath

    try {
      val hash = MessageDigest getInstance "SHA-256"
      val input = url.openConnection.getInputStream
      val w = new FileOutputStream(tmpFile)

      Stream.continually(input.read).takeWhile(_ != -1).foreach(w.write)

      hash update (Files readAllBytes path)
      input.close()

      Files.delete(path)

      Success {
        hash.digest() map { "%02X" format _ } mkString
      }
    } catch { case e: IOException =>
      Files.delete(path)
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

