addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.4.9")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25"
