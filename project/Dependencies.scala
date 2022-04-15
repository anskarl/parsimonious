import sbt._
import sbt.Keys._

object Dependencies {
    object v {
        final val Spark = "2.4.5"
        final val Hadoop = "2.10.0"
        final val SparkTestingBase = "2.4.5_0.14.0"

        final val Parquet = "1.10.1"
//        final val Thrift = "0.10.0"
        final val Thrift = "0.13.0"

        final val ScalaTest = "3.1.4"
        final val ScalaTestPlus = "3.1.4.0"
        final val ScalaCheck = "1.14.0"

        final val UtilBackports = "2.1"
        final val SLF4J = "1.7.25"
        final val Jackson = "2.13.0"

        final val ScalaCollectionCompat = "2.1.6"

        final val JavaXAnnotationApi = "1.3.2"
    }

    lazy val JavaXAnnotationApi = "javax.annotation" % "javax.annotation-api" % v.JavaXAnnotationApi

    lazy val ScalaCollectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % v.ScalaCollectionCompat

    lazy val UtilBackports = "com.github.bigwheel" %% "util-backports" % v.UtilBackports

    lazy val ScalaCheck = "org.scalacheck" %% "scalacheck" % v.ScalaCheck % Test
    lazy val ScalaTest = Seq(
        "org.scalatest" %% "scalatest" % v.ScalaTest % Test,
        "org.scalatestplus" %% "scalacheck-1-14" % v.ScalaTestPlus % Test
    )

    lazy val Spark = Seq(
        "org.apache.spark" %% "spark-core" % v.Spark % Provided exclude("org.apache.hadoop", "hadoop-client"),
        "org.apache.spark" %% "spark-sql" % v.Spark % Provided exclude("org.apache.hadoop", "hadoop-client"),
        "org.apache.spark" %% "spark-catalyst" % v.Spark % Provided exclude("org.apache.hadoop", "hadoop-client")
    )

    lazy val SparkTestingBase = "com.holdenkarau" %% "spark-testing-base" % v.SparkTestingBase % Test

//    lazy val Jackson = "com.fasterxml.jackson.core" % "jackson-databind" % v.Jackson

    lazy val Hadoop = Seq(
        "org.apache.hadoop" % "hadoop-client" % v.Hadoop % Provided,
        "org.apache.hadoop" % "hadoop-hdfs" % v.Hadoop % Provided
    )

    lazy val ParquetThrift = "org.apache.parquet" % "parquet-thrift" % v.Parquet excludeAll(
        ExclusionRule("org.slf4j", "slf4j-api"),
        ExclusionRule("org.slf4j", "slf4j-log4j12"),
        ExclusionRule("org.slf4j", "log4j-over-slf4j"),
        ExclusionRule("org.slf4j", "jcl-over-slf4j"),
        ExclusionRule("org.slf4j", "jul-to-slf4j")
    )

    lazy val Thrift = "org.apache.thrift" % "libthrift" % v.Thrift excludeAll (
      ExclusionRule("org.apache.httpcomponents", "httpclient"),
      ExclusionRule("org.apache.httpcomponents", "httpcore"),
      ExclusionRule("org.slf4j", "slf4j-api"))


    lazy val TestDependencies = Seq(
        "org.slf4j" % "slf4j-api" % v.SLF4J % Test,
        "org.slf4j" % "slf4j-nop" % v.SLF4J % Test,
        "org.scalatest" %% "scalatest" % v.ScalaTest % Test,
        "org.scalacheck" %% "scalacheck" % v.ScalaCheck % Test,
        "org.apache.hadoop" % "hadoop-minicluster" % v.Hadoop % Test,
        "org.apache.spark" %% "spark-core" % v.Spark % Test,
        "org.apache.spark" %% "spark-sql" % v.Spark % Test,
        "org.apache.spark" %% "spark-streaming" % v.Spark % Test,
        "org.apache.spark" %% "spark-catalyst" % v.Spark % Test
    )
}