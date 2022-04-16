import sbt._
import sbt.Keys._
import com.intenthq.sbt.ThriftPlugin._

val gihubPackageSettings = Seq(
  githubOwner := "anskarl",
  githubRepository := "parsimonious",
  githubTokenSource := TokenSource.GitConfig("github.token")
)

def module(name: String) = Project(s"parsimonious-${name}", file(name))
  .settings(
    fork in Test := true,
    javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled"),
    parallelExecution in Test := false,
  )
  .settings(gihubPackageSettings)
  .settings(libraryDependencies ++= Dependencies.ScalaTest)
  .settings(libraryDependencies ++= Dependencies.TestDependencies)
  .settings(libraryDependencies += Dependencies.ScalaCheck)

lazy val commons = module("commons")
  .settings(Seq(publish := {}, publishLocal := {}))
  .settings(libraryDependencies += Dependencies.Thrift)
  .settings(libraryDependencies += Dependencies.JavaXAnnotationApi)
  .settings(libraryDependencies += Dependencies.UtilBackports)
  .settings(libraryDependencies += Dependencies.ScalaCollectionCompat)
  .settings(
//    Thrift / thrift := s"docker run -v ${file(".").getAbsoluteFile.toString}:${file(".").getAbsoluteFile.toString} --workdir ${file(".").getAbsoluteFile.toString} jaegertracing/thrift:0.13 thrift",
    Thrift / thrift := s"docker run --rm  -v ${file(".").getAbsoluteFile.toString}:${file(".").getAbsoluteFile.toString} --workdir ${file(".").getAbsoluteFile.toString} anskarl/thrift:0.10.0",
    Thrift / thriftSourceDir := file("."),
    Thrift / thriftJavaOptions := Seq(s" -gen java: -I ${(file(".") / "src" / "test"/ "thrift").getPath}"),
    dependencyOverrides += "org.apache.thrift" % "libthrift" % Dependencies.v.Thrift,
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % Dependencies.v.Jackson,
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % Dependencies.v.Jackson
  )

lazy val json = module("jackson")
  .dependsOn(commons % "compile->compile;test->test")
  .settings(scalaVersion := "2.12.12")
  .settings(crossScalaVersions := Seq("2.12.12", "2.13.8"))
  .settings(libraryDependencies ++= Dependencies.Jackson)


lazy val spark = module("spark")
  .dependsOn(commons % "compile->compile;test->test")
  .settings(scalaVersion := "2.12.12")
  .settings(resolvers += ("Twitter Maven Repo" at "http://maven.twttr.com").withAllowInsecureProtocol(true))
  .settings(libraryDependencies ++= Dependencies.sparkDependenciesFor("spark3"))

lazy val root = Project("parsimonious", file("."))
  .settings(scalaVersion := "2.12.12")
  .settings(Seq(publish := {}, publishLocal := {}))
  .settings(gihubPackageSettings)
  .aggregate(commons, spark, json)
