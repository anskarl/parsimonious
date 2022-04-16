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

lazy val thriftCommons = module("thrift-commons")
  .settings(Seq(publish := {}, publishLocal := {}))
  .settings(libraryDependencies += Dependencies.Thrift)
  .settings(libraryDependencies += Dependencies.JavaXAnnotationApi)
  .settings(libraryDependencies ++= Dependencies.TestDependencies)
  .settings(libraryDependencies += Dependencies.UtilBackports)
  .settings(libraryDependencies += Dependencies.ScalaCheck)
  .settings(libraryDependencies += Dependencies.ScalaCollectionCompat)
  .settings(libraryDependencies ++= Dependencies.Jackson)
  .settings(
    Thrift / thrift := s"docker run -v ${file(".").getAbsoluteFile.toString}:${file(".").getAbsoluteFile.toString} --workdir ${file(".").getAbsoluteFile.toString} jaegertracing/thrift:0.13 thrift",
    Thrift / thriftSourceDir := file("."),
    Thrift / thriftJavaOptions += s"  -I ${(file(".") / "src" / "test"/ "thrift").getPath}",
    dependencyOverrides += "org.apache.thrift" % "libthrift" % Dependencies.v.Thrift,
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % Dependencies.v.Jackson,
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % Dependencies.v.Jackson
  )

lazy val json = module("thrift-jackson")
  .dependsOn(thriftCommons % "compile->compile;test->test")
  .settings(scalaVersion := "2.12.12")
  .settings(libraryDependencies ++= Dependencies.ScalaTest)
  .settings(libraryDependencies ++= Dependencies.TestDependencies)
  .settings(libraryDependencies += Dependencies.Thrift)
  .settings(libraryDependencies += Dependencies.UtilBackports)
  .settings(libraryDependencies += Dependencies.ScalaCheck)
  .settings(libraryDependencies += Dependencies.ScalaCollectionCompat)
  .settings(libraryDependencies += Dependencies.JavaXAnnotationApi)

lazy val spark = module("thrift-spark")
  .dependsOn(thriftCommons % "compile->compile;test->test")
  .settings(scalaVersion := "2.12.12")
  .settings(resolvers += ("Twitter Maven Repo" at "http://maven.twttr.com").withAllowInsecureProtocol(true))
  .settings(libraryDependencies ++= Dependencies.Spark)
  .settings(libraryDependencies ++= Dependencies.Hadoop)
  .settings(libraryDependencies ++= Dependencies.ScalaTest)
  .settings(libraryDependencies ++= Dependencies.TestDependencies)
  .settings(libraryDependencies ++= Dependencies.TestDependenciesSpark)
  .settings(libraryDependencies += Dependencies.SparkTestingBase)
  .settings(libraryDependencies += Dependencies.Thrift)
  .settings(libraryDependencies += Dependencies.ParquetThrift)
  .settings(libraryDependencies += Dependencies.UtilBackports)
  .settings(libraryDependencies += Dependencies.ScalaCheck)
  .settings(libraryDependencies += Dependencies.ScalaCollectionCompat)
  .settings(libraryDependencies += Dependencies.JavaXAnnotationApi)

lazy val root = Project("parsimonious", file("."))
  .settings(scalaVersion := "2.12.12")
  .settings(Seq(publish := {}, publishLocal := {}))
  .settings(gihubPackageSettings)
  .aggregate(thriftCommons, spark, json)
