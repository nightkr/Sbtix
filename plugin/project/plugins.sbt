libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15-1")

resolvers += Resolver.typesafeIvyRepo("releases")
