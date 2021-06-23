import sbt._
import sbt.Keys._
import com.intenthq.sbt.ThriftPlugin._


lazy val root = Project("parsimonious", file("."))
  .settings(scalaVersion := "2.12.12")
  .settings(crossScalaVersions := Seq("2.11.12", "2.12.12"))
  .settings(resolvers += ("Twitter Maven Repo" at "http://maven.twttr.com").withAllowInsecureProtocol(true))
  .settings(
    fork in Test := true,
    javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled"),
    parallelExecution in Test := false
  )
  .settings(libraryDependencies ++= Dependencies.Spark)
  .settings(libraryDependencies ++= Dependencies.Hadoop)
  .settings(libraryDependencies ++= Dependencies.ScalaTest)
  .settings(libraryDependencies ++= Dependencies.TestDependencies)
  .settings(libraryDependencies += Dependencies.SparkTestingBase)
  .settings(libraryDependencies += Dependencies.Thrift)
  .settings(libraryDependencies += Dependencies.ParquetThrift)
  .settings(libraryDependencies += Dependencies.UtilBackports)
  .settings(libraryDependencies += Dependencies.ScalaCheck)
  .settings(libraryDependencies += Dependencies.Jackson)
  .settings(libraryDependencies += Dependencies.ScalaCollectionCompat)
  .settings(
    Thrift / thrift := s"docker run --rm  -v ${file(".").getAbsoluteFile.toString}:${file(".").getAbsoluteFile.toString} --workdir ${file(".").getAbsoluteFile.toString} anskarl/thrift:0.10.0 ",
    Thrift / thriftSourceDir := file("."),
    Thrift / thriftJavaOptions += s" -I ${(file(".") / "src" / "test"/ "thrift").getPath}",
    dependencyOverrides += "org.apache.thrift" % "libthrift" % Dependencies.v.Thrift,
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % Dependencies.v.Jackson,
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % Dependencies.v.Jackson,
    githubOwner := "anskarl",
    githubRepository := "parsimonious",
    githubTokenSource := TokenSource.GitConfig("github.token")
  )