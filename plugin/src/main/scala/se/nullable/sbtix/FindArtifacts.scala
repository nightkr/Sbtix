package se.nullable.sbtix

import java.net.URL
import java.util.concurrent.Semaphore

import sbt.Logger

import scala.sys.process._

object FindArtifactsOfRepo {
  private val semaphore = new Semaphore(4, false)

  def fetchChecksum(originalUrl: String, artifactType: String, url: URL): String = {

    val procLogger = sys.process.ProcessLogger {
      mess =>
        if (mess.startsWith("path is")) {
          val modMess = mess.replace("path is ", "")
          println(s"$artifactType ${originalUrl} => $modMess")
        } else if (mess.startsWith("error: unable to download")) {
          System.err.println(mess)
        } else {
          System.err.println(mess)
        }
    }

    semaphore.acquireUninterruptibly()
    val checksum = Seq("nix-prefetch-url", url.toString, "--type", "sha256").!!(procLogger).trim()
    semaphore.release()
    checksum
  }
}

class FindArtifactsOfRepo(repoName: String, root: String) {

  def findArtifacts(logger: Logger, modules: Set[GenericModule]): Set[NixArtifact] = modules.flatMap { ga =>
    val rootUrl = new URL(root);

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
        FindArtifactsOfRepo.fetchChecksum(calcUrl.toString, "Artifact", artifactLocalFile.toURI.toURL))
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

