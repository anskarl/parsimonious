import sbt._
import sbt.Keys._

object Dependencies {

    object v {
        final val Spark2 = "2.4.8"
        final val Hadoop2 = "2.10.0"
        final val SparkTestingBase2 = "2.4.5_0.14.0"
        final val Parquet10 = "1.10.1"

        final val Spark3 = "3.2.0"
        final val Hadoop3 = "3.0.0"
        final val SparkTestingBase3 = "3.2.0_1.1.1"
        final val Parquet12 = "1.12.2"


        final val Thrift = "0.10.0"
        // final val Thrift = "0.13.0"

        final val ScalaTest = "3.1.4"
        final val ScalaTestPlus = "3.1.4.0"
        final val ScalaCheck = "1.14.0"

        final val UtilBackports = "2.1"
        final val SLF4J = "1.7.25"
        final val Jackson = "2.13.0"

        final val ScalaCollectionCompat = "2.1.6"

        final val JavaXAnnotationApi = "1.3.2"
    }


    def sparkDependenciesFor(profile: String): Seq[ModuleID] ={
        val (sparkVersion, hadoopVersion, sparkTestingBaseVersion, parquetVersion) = profile match {
            case "spark2" => (v.Spark2, v.Hadoop2, v.SparkTestingBase2, v.Parquet10)
            case "spark3" => (v.Spark3, v.Hadoop3, v.SparkTestingBase3, v.Parquet12)
            case _ => throw new IllegalArgumentException(s"Unknown profile name '$profile'")
        }

        Seq(
            // Hadoop
            "org.apache.hadoop" % "hadoop-client" % hadoopVersion % Provided,
            "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % Provided,
            // Test dependencies Hadoop
            "org.apache.hadoop" % "hadoop-minicluster" % hadoopVersion % Test,
            // Spark
            "org.apache.spark" %% "spark-core" % sparkVersion % Provided exclude("org.apache.hadoop", "hadoop-client"),
            "org.apache.spark" %% "spark-sql" % sparkVersion % Provided exclude("org.apache.hadoop", "hadoop-client"),
            "org.apache.spark" %% "spark-catalyst" % sparkVersion % Provided exclude("org.apache.hadoop", "hadoop-client"),
            // Test dependencies Spark
            "org.apache.spark" %% "spark-core" % sparkVersion % Test,
            "org.apache.spark" %% "spark-sql" % sparkVersion % Test,
            "org.apache.spark" %% "spark-streaming" % sparkVersion % Test,
            "org.apache.spark" %% "spark-catalyst" % sparkVersion % Test,
            "com.holdenkarau" %% "spark-testing-base" % sparkTestingBaseVersion % Test,
            // Parquet
            "org.apache.parquet" % "parquet-thrift" % parquetVersion excludeAll(
              ExclusionRule("org.slf4j", "slf4j-api"),
              ExclusionRule("org.slf4j", "slf4j-log4j12"),
              ExclusionRule("org.slf4j", "log4j-over-slf4j"),
              ExclusionRule("org.slf4j", "jcl-over-slf4j"),
              ExclusionRule("org.slf4j", "jul-to-slf4j")
            )
        )
    }

    lazy val JavaXAnnotationApi = "javax.annotation" % "javax.annotation-api" % v.JavaXAnnotationApi

    lazy val ScalaCollectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % v.ScalaCollectionCompat

    lazy val UtilBackports = "com.github.bigwheel" %% "util-backports" % v.UtilBackports

    lazy val ScalaCheck = "org.scalacheck" %% "scalacheck" % v.ScalaCheck % Test
    lazy val ScalaTest = Seq(
        "org.scalatest" %% "scalatest" % v.ScalaTest % Test,
        "org.scalatestplus" %% "scalacheck-1-14" % v.ScalaTestPlus % Test
    )

    lazy val Jackson = Seq(
        "com.fasterxml.jackson.core" % "jackson-core" % v.Jackson,
        "com.fasterxml.jackson.core" % "jackson-databind" % v.Jackson,
        "com.fasterxml.jackson.core" % "jackson-annotations" % v.Jackson,
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % v.Jackson
    )

    lazy val Thrift = "org.apache.thrift" % "libthrift" % v.Thrift excludeAll (
      ExclusionRule("org.apache.httpcomponents", "httpclient"),
      ExclusionRule("org.apache.httpcomponents", "httpcore"),
      ExclusionRule("org.slf4j", "slf4j-api"))

    lazy val TestDependencies = Seq(
        "org.slf4j" % "slf4j-api" % v.SLF4J % Test,
        "org.slf4j" % "slf4j-nop" % v.SLF4J % Test,
        "org.scalatest" %% "scalatest" % v.ScalaTest % Test,
        "org.scalacheck" %% "scalacheck" % v.ScalaCheck % Test
    )
}