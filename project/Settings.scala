import sbt.{ Def, _ }
import sbt.Keys._
import sbt.TestFrameworks.Specs2
import sbt.Tests.Argument
import com.typesafe.sbt._
import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import sbtassembly.AssemblyPlugin.autoImport._
import scoverage._
import spray.revolver.RevolverPlugin.autoImport._
import wartremover.WartRemover.autoImport._

object Settings extends Dependencies {

  val FunctionalTest: Configuration = config("fun") extend Test describedAs "Runs only functional tests"

  private val commonSettings = Seq(
    organization := "io.branchtalk",
    scalaOrganization := scalaOrganizationUsed,
    scalaVersion := scalaVersionUsed,
    crossScalaVersions := crossScalaVersionsUsed
  )

  private val rootSettings = commonSettings

  private val modulesSettings = commonSettings ++ Seq(
    scalacOptions ++= Seq(
      // standard settings
      "-target:jvm-1.8",
      "-encoding",
      "UTF-8",
      "-unchecked",
      "-deprecation",
      "-explaintypes",
      "-feature",
      // language features
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      // private options
      "-Xexperimental",
      "-Ybackend-parallelism",
      "8",
      "-Ymacro-annotations",
      "-Yno-adapted-args",
      "-Ypartial-unification",
      // warnings
      "-Ywarn-dead-code",
      "-Ywarn-extra-implicit",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-macros:after",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen",
      //"-Ywarn-unused:implicits",
      //"-Ywarn-unused:patvars",
      //"-Ywarn-unused:privates",
      "-Ywarn-value-discard",
      // advanced options
      "-Xcheckinit",
      "-Xfatal-warnings",
      "-Xfuture",
      // linting
      "-Xlint:adapted-args",
      "-Xlint:by-name-right-associative",
      "-Xlint:constant",
      "-Xlint:delayedinit-select",
      "-Xlint:doc-detached",
      "-Xlint:inaccessible",
      "-Xlint:infer-any",
      "-Xlint:missing-interpolator",
      "-Xlint:nullary-override",
      "-Xlint:nullary-unit",
      "-Xlint:option-implicit",
      "-Xlint:package-object-classes",
      "-Xlint:poly-implicit-overload",
      "-Xlint:private-shadow",
      "-Xlint:stars-align",
      "-Xlint:type-parameter-shadow",
      "-Xlint:unsound-match"
    ).filterNot(
      (if (scalaVersion.value.startsWith("2.13"))
         Set(
           // removed in 2.13.x
           "-Yno-adapted-args",
           "-Ypartial-unification",
           "-Ywarn-inaccessible",
           "-Ywarn-infer-any",
           "-Ywarn-nullary-override",
           "-Ywarn-nullary-unit",
           "-Xlint:by-name-right-associative",
           "-Xlint:nullary-override",
           "-Xlint:unsound-match",
           "-Xfuture",
           // only for 2.11.x
           "-Xexperimental"
         )
       else if (scalaVersion.value.startsWith("2.12"))
         Set(
           // added in 2.13.x
           "-Ymacro-annotations",
           // only for 2.11.x
           "-Xexperimental"
         )
       else if (scalaVersion.value.startsWith("2.11"))
         Set(
           // added in 2.13.x
           "-Ymacro-annotations",
           // added in 2.12.x
           "-Ybackend-parallelism",
           "8",
           "-Ywarn-extra-implicit",
           "-Ywarn-macros:after",
           "-Ywarn-unused:implicits",
           "-Ywarn-unused:patvars",
           "-Ywarn-unused:privates",
           "-Xlint:constant"
         )
       else Set.empty[String]).contains _
    ),
    console / scalacOptions --= Seq(
      // warnings
      "-Ywarn-unused:implicits",
      "-Ywarn-unused:imports",
      "-Ywarn-unused:locals",
      "-Ywarn-unused:params",
      "-Ywarn-unused:patvars",
      "-Ywarn-unused:privates",
      // advanced options
      "-Xfatal-warnings",
      // linting
      "-Xlint"
    ),
    Global / cancelable := true,
    Compile / trapExit := false,
    Compile / connectInput := true,
    Compile / outputStrategy := Some(StdoutOutput),
    resolvers ++= commonResolvers,
    libraryDependencies ++= mainDeps,
    addCompilerPlugin(Dependencies.betterMonadicFor),
    addCompilerPlugin(Dependencies.kindProjector),
    Compile / scalafmtOnCompile := true,
    Compile / compile / wartremoverWarnings ++= Warts.allBut(
      Wart.Any,
      Wart.DefaultArguments,
      Wart.ExplicitImplicitTypes,
      Wart.ImplicitConversion,
      Wart.ImplicitParameter,
      Wart.Overloading,
      Wart.PublicInference,
      Wart.NonUnitStatements,
      Wart.Nothing
    )
  )

  def customPredef(imports: String*): Def.Setting[Task[Seq[String]]] =
    scalacOptions += s"-Yimports:${(Seq("java.lang", "scala", "scala.Predef") ++ imports).mkString(",")}"

  implicit final class RunConfigurator(project: Project) {

    def configureRun(main: String): Project =
      project
        .settings(
          inTask(assembly)(
            Seq(
              assemblyJarName := s"${name.value}.jar",
              assemblyMergeStrategy := {
                case strategy => MergeStrategy.defaultMergeStrategy(strategy)
              },
              mainClass := Some(main)
            )
          )
        )
        .settings(Compile / run / mainClass := Some(main))
  }

  sealed abstract class TestConfigurator(project: Project, config: Configuration) {

    protected def configure(requiresFork: Boolean): Project =
      project
        .configs(config)
        .settings(inConfig(config)(Defaults.testSettings): _*)
        .settings(inConfig(config)(scalafmtConfigSettings))
        .settings(
          inConfig(config)(
            Seq(
              scalafmtOnCompile := true,
              fork := requiresFork,
              testFrameworks := Seq(Specs2)
            )
          )
        )
        .settings(libraryDependencies ++= testDeps map (_ % config.name))
        .enablePlugins(ScoverageSbtPlugin)

    protected def configureSequential(requiresFork: Boolean): Project =
      configure(requiresFork).settings(
        inConfig(config)(
          Seq(
            testOptions += Argument(Specs2, "sequential"),
            parallelExecution := false
          )
        )
      )
  }

  implicit final class DataConfigurator(project: Project) {

    def setName(newName: String): Project = project.settings(name := newName)

    def setDescription(newDescription: String): Project = project.settings(description := newDescription)

    def setInitialImport(newInitialCommand: String*): Project =
      project.settings(initialCommands := s"import ${("io.branchtalk._" +: newInitialCommand).mkString(", ")}")
  }

  implicit final class RootConfigurator(project: Project) {

    def configureRoot: Project = project.settings(rootSettings: _*)
  }

  implicit final class ModuleConfigurator(project: Project) {

    def configureModule: Project = project.settings(modulesSettings: _*).enablePlugins(GitVersioning)
  }

  implicit final class UnitTestConfigurator(project: Project) extends TestConfigurator(project, Test) {

    def configureTests(requiresFork: Boolean = false): Project = configure(requiresFork)

    def configureTestsSequential(requiresFork: Boolean = false): Project = configureSequential(requiresFork)
  }

  implicit final class FunctionalTestConfigurator(project: Project) extends TestConfigurator(project, FunctionalTest) {

    def configureFunctionalTests(requiresFork: Boolean = false): Project = configure(requiresFork)

    def configureFunctionalTestsSequential(requiresFork: Boolean = false): Project = configureSequential(requiresFork)
  }

  implicit final class IntegrationTestConfigurator(project: Project)
      extends TestConfigurator(project, IntegrationTest) {

    def configureIntegrationTests(requiresFork: Boolean = false): Project = configure(requiresFork)

    def configureIntegrationTestsSequential(requiresFork: Boolean = false): Project = configureSequential(requiresFork)
  }
}
