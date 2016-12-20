if (sys.props.contains("plugin.version")) {
  Seq(addSbtPlugin("se.nullable.sbtix" % "sbtix" % sys.props("plugin.version")))
} else {
  Seq()
}
