import sbt._
import sbt.Keys._
import com.intenthq.sbt.ThriftPlugin._

val thriftVersion = sys.env.getOrElse("THRIFT_VERSION", "0.10.0")
val thriftMajorVersion = thriftVersion.substring(0, thriftVersion.lastIndexOf("."))
val sparkProfile = sys.env.getOrElse("SPARK_PROFILE", "spark3").toLowerCase()
val releaseToSonatype = sys.env.getOrElse("RELEASE_SONATYPE", "false").toBoolean
val DefaultScalaVersion = "2.12.12"
val DefaultCrossScalaVersions = Seq("2.12.12", "2.13.8")

val commonSettings = Seq(
  githubOwner := "anskarl",
  githubRepository := "parsimonious",
  githubTokenSource := {
    if (sys.env.contains("GITHUB_TOKEN")) TokenSource.Environment("GITHUB_TOKEN")
    else TokenSource.GitConfig("github.token")
  },
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
  credentials ++= (if(releaseToSonatype) Seq(Credentials(Path.userHome / ".sbt" / "sonatype_credentials")) else Seq.empty),
  publishTo := (if(releaseToSonatype) sonatypePublishToBundle.value else githubPublishTo.value),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(
    ScmInfo(url("https://github.com/anskarl/parsimonious"), "scm:git:git@github.com:anskarl/parsimonious.git")
  ),
  pomExtra :=
    <url>https://github.com/anskarl</url>
      <licenses>
        <license>
          <name>Apache License Version 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        </license>
      </licenses>
      <developers>
        <developer>
          <id>anskarl</id>
          <name>Anastasios Skarlatidis</name>
          <url>https://anskarl.github.io</url>
        </developer>
      </developers>
)

def thriftCmd(majorVersion: String): String = majorVersion match {
  case "0.10" =>
    s"docker run --rm  -v ${file(".").getAbsoluteFile.toString}:${file(".").getAbsoluteFile.toString} --workdir ${file(".").getAbsoluteFile.toString} anskarl/thrift:0.10.0"
  case _ =>
    s"docker run -v ${file(".").getAbsoluteFile.toString}:${file(".").getAbsoluteFile.toString} --workdir ${file(".").getAbsoluteFile.toString} jaegertracing/thrift:${majorVersion} thrift"
}

def module(name: String, versionPrefix: String) =
  Project(s"parsimonious-${name}", file(name))
    .settings(version := s"${versionPrefix}-${version.value}")
    .settings(scalaVersion := DefaultScalaVersion)
    .settings(
      organization := "com.github.anskarl",
      publishMavenStyle := true
    )
    .settings(
      Test / fork := true,
      javaOptions ++= Seq("-Xms512M", "-Xmx2048M"),
      Test / parallelExecution := false,
    )
    .settings(commonSettings)
    .settings(libraryDependencies ++= Dependencies.ScalaTest)
    .settings(libraryDependencies ++= Dependencies.TestDependencies)
    .settings(libraryDependencies += Dependencies.ScalaCheck)

lazy val commons = module("commons", s"thrift_${thriftMajorVersion}")
  .settings(crossScalaVersions := DefaultCrossScalaVersions)
  .settings(libraryDependencies += Dependencies.thrift(thriftVersion))
  .settings(libraryDependencies += Dependencies.JavaXAnnotationApi)
  .settings(libraryDependencies += Dependencies.UtilBackports)
  .settings(libraryDependencies += Dependencies.ScalaCollectionCompat)
  .settings(
    Thrift / thrift := thriftCmd(thriftMajorVersion),
    Thrift / thriftSourceDir := file("."),
    Thrift / thriftJavaOptions := Seq(s" -gen java: -I ${(file(".") / "src" / "test"/ "thrift").getPath}"),
    dependencyOverrides += "org.apache.thrift" % "libthrift" % thriftVersion,
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % Dependencies.v.Jackson,
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % Dependencies.v.Jackson
  )

lazy val jackson = module("jackson", s"thrift_${thriftMajorVersion}")
  .dependsOn(commons % "compile->compile;test->test")
  .settings(crossScalaVersions := DefaultCrossScalaVersions)
  .settings(libraryDependencies ++= Dependencies.Jackson)

lazy val spark = module("spark", s"thrift_${thriftMajorVersion}_${sparkProfile}")
  .dependsOn(commons % "compile->compile;test->test")
  .settings(crossScalaVersions := (if(sparkProfile == "spark2") Seq(DefaultScalaVersion) else DefaultCrossScalaVersions  ))
  .settings(resolvers += ("Twitter Maven Repo" at "http://maven.twttr.com").withAllowInsecureProtocol(true))
  .settings(libraryDependencies ++= Dependencies.sparkDependenciesFor(sparkProfile))
  .settings(libraryDependencies ++= Dependencies.Jackson)

lazy val root = Project("parsimonious", file("."))
  .settings(scalaVersion := DefaultScalaVersion)
  .settings(Seq(publish := {}, publishLocal := {}, publish / skip := true))
  .settings(commonSettings)
  .aggregate(commons, spark, jackson)
