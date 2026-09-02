// git
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.1.0")
// linters
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.6.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4")
// project matrix is in-sourced into sbt 2.x (no sbt-projectmatrix plugin needed)
// publishing
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.5.0")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.7")
// disabling projects in IDE (groupId moved to org.jetbrains.scala for the sbt 2 build)
addSbtPlugin("org.jetbrains.scala" % "sbt-ide-settings" % "1.1.4")
// NOTE: io.spray:sbt-revolver has no sbt 2 build yet - dropped (dev-only convenience)

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25"
