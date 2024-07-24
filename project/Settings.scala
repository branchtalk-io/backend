import sbt._
import sbt.Keys._
import sbt.TestFrameworks.Specs2
import sbt.Tests.Argument
import com.github.sbt.git.GitVersioning
import com.typesafe.sbt._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.DockerPlugin
import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport.JVMPlatform
import sbtcrossproject.CrossProject
import scoverage._
import wartremover.WartRemover.autoImport._

object Settings extends Dependencies {

  private val commonSettings = Seq(
    organization := "io.branchtalk",
    scalaVersion := scalaVersionUsed,
    crossScalaVersions := crossScalaVersionsUsed,
    // kind of required to avoid "{project}/doc" task failure, because of Catnip :/
    Compile / doc / sources := Seq.empty,
    Compile / packageDoc / publishArtifact := false
  )

  private val rootSettings = commonSettings

  private val modulesSettings = commonSettings ++ Seq(
    scalacOptions ++= Seq(
      // standard settings
      // format: off
      "-encoding", "UTF-8",
      "-rewrite",
      "-source", "3.3-migration",
      // format: on
      "-unchecked",
      "-deprecation",
      "-explaintypes",
      "-feature",
      "-Wnonunit-statement",
      "-Wvalue-discard",
      //"-Xfatal-warnings",
      "-Ykind-projector:underscores"
    ),
    console / scalacOptions --= Seq(
      // warnings
      "-Ywarn-unused",
      // "-Wunused:imports", // import x.Underlying as X is marked as unused even though it is! probably one of https://github.com/scala/scala3/issues/: #18564, #19252, #19657, #19912
      "-Wunused:privates",
      "-Wunused:locals",
      "-Wunused:explicits",
      "-Wunused:implicits",
      "-Wunused:params",
      "-Wvalue-discard",
      "-Xfatal-warnings",
      "-Xcheck-macros",
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
        .enablePlugins(JavaAppPackaging, DockerPlugin)
        .settings(
          inTask(assembly)(
            Seq(
              assemblyJarName := s"${name.value}.jar",
              assemblyMergeStrategy := {
                // required for OpenAPIServer to work
                case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") =>
                  MergeStrategy.singleOrError
                // conflicts on random crap
                case "module-info.class" => MergeStrategy.discard
                // otherwise
                case strategy => MergeStrategy.defaultMergeStrategy(strategy)
              },
              mainClass := Some(main)
            )
          )
        )
        .settings(
          Compile / run / mainClass := Some(main),
          Compile / run / fork := true,
          Compile / runMain / fork := true
        )
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

  implicit final class CrossDataConfigurator(project: CrossProject) {

    def setName(newName: String): CrossProject = project.configure(_.setName(newName))

    def setDescription(newDescription: String): CrossProject = project.configure(_.setDescription(newDescription))

    def setInitialImport(newInitialCommand: String*): CrossProject =
      project.configure(_.setInitialImport(newInitialCommand: _*))
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

  implicit final class IntegrationTestConfigurator(project: Project)
      extends TestConfigurator(project, IntegrationTest) {

    def configureIntegrationTests(requiresFork: Boolean = false): Project = configure(requiresFork)

    def configureIntegrationTestsSequential(requiresFork: Boolean = false): Project = configureSequential(requiresFork)
  }

  implicit final class CrossModuleConfigurator(project: CrossProject) {

    private val testConfigurations = Set("test", "fun", "it")
    private def findCompileAndTestConfigs(p: CrossProject) = {
      val names = p.projects(JVMPlatform).configurations.map(_.name).toSet intersect testConfigurations
      (p.projects(JVMPlatform).configurations.filter(cfg => names(cfg.name)).toSet) + Compile
    }

    def configureModule: CrossProject = project
      .configure(_.configureModule)
      .settings(
        // workaround for https://github.com/portable-scala/sbt-crossproject/issues/74
        findCompileAndTestConfigs(project).toList.flatMap(
          inConfig(_) {
            unmanagedResourceDirectories ++= {
              unmanagedSourceDirectories.value
                .map(src => (src / ".." / "resources").getCanonicalFile)
                .filterNot(unmanagedResourceDirectories.value.contains)
                .distinct
            }
          }
        )
      )
  }

  implicit final class CrossUnitTestConfigurator(project: CrossProject) {

    def configureTests(requiresFork: Boolean = false): CrossProject = project.configure(_.configureTests(requiresFork))

    def configureTestsSequential(requiresFork: Boolean = false): CrossProject =
      project.configure(_.configureTestsSequential(requiresFork))
  }

  implicit final class CrossIntegrationTestConfigurator(project: CrossProject) {

    def configureIntegrationTests(requiresFork: Boolean = false): CrossProject =
      project.configure(_.configureIntegrationTests(requiresFork))

    def configureIntegrationTestsSequential(requiresFork: Boolean = false): CrossProject =
      project.configure(_.configureIntegrationTestsSequential(requiresFork))
  }
}
