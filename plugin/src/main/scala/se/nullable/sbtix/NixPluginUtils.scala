package se.nullable

import java.net.URI
import java.io.File

import scala.io.Source

package object sbtix {
  def quote(s: String) = "\"" + s + "\""

  def indent(str: String) = {
    val spaces = " " * 2
    str.replace("\n", "\n" + spaces)
  }

  def parentURI(uri: URI) = if (uri.getPath().endsWith("/")) uri.resolve("..") else uri.resolve(".")

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

  def resource2string(file: String): String = {
    Source.fromInputStream(getClass.getResourceAsStream(file)).getLines mkString "\n"
  }
}
