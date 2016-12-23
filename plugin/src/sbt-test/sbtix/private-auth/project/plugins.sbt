if (sys.props.contains("plugin.version")) {
  Seq(addSbtPlugin("se.nullable.sbtix" % "sbtix" % sys.props("plugin.version")))
} else {
  Seq()
}

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15-1")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.4")
