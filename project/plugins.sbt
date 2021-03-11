// Scala.js and cross-compilation
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.5.0")
// publishing
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.8.0-RC14")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")
// linting
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("com.beautiful-scala" % "sbt-scalastyle" % "1.5.0")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.4.13")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
// running
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25"
