package se.nullable.sbtix

import java.io.File
import java.net.{URI, URL}

import sbt.ProjectRef
import coursier._
import coursier.core.Authentication
import sbt.{Logger, ModuleID}

import scala.sys.process._


object FindArtifacts {
 
    def apply(repoName:String,root:String)(logger:Logger, modules:Seq[GenericModule]) : Seq[NixArtifact] = {
      val rootUrl = new URL(root);

       

       def findArtifacts(url: URL, localFile: File, auth: Option[Authentication]): Seq[NixArtifact] = {

        def authed(url:URL) = {
          auth match {
              case Some(a) => 
                new URI(url.getProtocol, s"${a.user}:${a.password}", url.getHost, url.getPort, url.getPath, url.getQuery, url.getRef)
              case None => url.toURI
          }
        }

        val authedRootURI = authed(rootUrl)

        val authedUri = authed(url)

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

        targetArtifacts.map{ artifactLocalFile =>

          val calcUrl = calculateURI(artifactLocalFile).toURL

          logger.info(s"Fetching $calcUrl")

          NixArtifact(
            repoName,
            calcUrl.toString.replace(authedRootURI.toString,""),
            calcUrl,
            fetchChecksum(artifactLocalFile.toURI.toURL)
          )
        }.toSeq
      }

      def fetchChecksum(url: URL): String = {
        Seq("nix-prefetch-url", url.toString, "--type", "sha256").!!.trim()
      }

    modules.flatMap{ ga =>
        findArtifacts(new URL(ga.artifact.url),ga.localFile, ga.artifact.authentication)
    }
  }
}
