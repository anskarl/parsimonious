import sbt._
import sbt.Keys._
import com.intenthq.sbt.ThriftPlugin._

val thriftVersion = sys.env.getOrElse("THRIFT_VERSION", "0.10.0")
val thriftMajorVersion = thriftVersion.substring(0, thriftVersion.lastIndexOf("."))
val sparkProfile = sys.env.getOrElse("SPARK_PROFILE", "spark3").toLowerCase()

val DefaultScalaVersion = "2.12.12"
val DefaultCrossScalaVersions = Seq("2.12.12", "2.13.8")

val githubPackageSettings = Seq(
  githubOwner := "anskarl",
  githubRepository := "parsimonious",
  githubTokenSource := {
    if (sys.env.contains("GITHUB_TOKEN")) TokenSource.Environment("GITHUB_TOKEN")
    else TokenSource.GitConfig("github.token")
  }
)

def thriftCmd(majorVersion: String): String = majorVersion match {
  case "0.10" =>
    s"docker run --rm  -v ${file(".").getAbsoluteFile.toString}:${file(".").getAbsoluteFile.toString} --workdir ${file(".").getAbsoluteFile.toString} anskarl/thrift:0.10.0"
  case _ =>
    s"docker run -v ${file(".").getAbsoluteFile.toString}:${file(".").getAbsoluteFile.toString} --workdir ${file(".").getAbsoluteFile.toString} jaegertracing/thrift:${majorVersion} thrift"
}

def module(name: String) = Project(s"parsimonious-${name}", file(name))
  .settings(scalaVersion := DefaultScalaVersion)
  .settings(
    organization := "io.github.anskarl",
    publishMavenStyle := true
  )
  .settings(
    fork in Test := true,
    javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled"),
    parallelExecution in Test := false,
  )
  .settings(githubPackageSettings)
  .settings(libraryDependencies ++= Dependencies.ScalaTest)
  .settings(libraryDependencies ++= Dependencies.TestDependencies)
  .settings(libraryDependencies += Dependencies.ScalaCheck)

lazy val commons = module("commons")
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
  .settings(version := s"thrift_${thriftMajorVersion}_${version.value}")
  .settings(
    artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
      s"${artifact.name}_${sv.binary}-${module.revision}.${artifact.extension}"
    }
  )

lazy val jackson = module("jackson")
  .dependsOn(commons % "compile->compile;test->test")
  .settings(crossScalaVersions := DefaultCrossScalaVersions)
  .settings(libraryDependencies ++= Dependencies.Jackson)
  .settings(version := s"thrift_${thriftMajorVersion}_${version.value}")
  .settings(
    artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
      s"${artifact.name}_${sv.binary}-${module.revision}.${artifact.extension}"
    }
  )

lazy val spark = module("spark")
  .dependsOn(commons % "compile->compile;test->test")
  .settings(crossScalaVersions := (if(sparkProfile == "spark2") Seq(DefaultScalaVersion) else DefaultCrossScalaVersions  ))
  .settings(resolvers += ("Twitter Maven Repo" at "http://maven.twttr.com").withAllowInsecureProtocol(true))
  .settings(libraryDependencies ++= Dependencies.sparkDependenciesFor(sparkProfile))
  .settings(libraryDependencies ++= Dependencies.Jackson)
  .settings(version := s"thrift_${thriftMajorVersion}_${sparkProfile}_${version.value}")
  .settings(
    artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
      s"${artifact.name}_${sv.binary}-${module.revision}.${artifact.extension}"
    }
  )

lazy val root = Project("parsimonious", file("."))
  .settings(scalaVersion := DefaultScalaVersion)
  .settings(Seq(publish := {}, publishLocal := {}, publish / skip := true))
  .settings(githubPackageSettings)
  .aggregate(commons, spark, jackson)
