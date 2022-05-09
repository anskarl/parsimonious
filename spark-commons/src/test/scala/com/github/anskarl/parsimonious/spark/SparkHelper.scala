package com.github.anskarl.parsimonious.spark

import java.io.File
import java.nio.file.Files
import java.util.UUID
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext, SparkContextHelper}
import org.apache.commons.io.FileUtils

import scala.util.chaining.scalaUtilChainingOps

object SparkHelper {

  def forceStopActiveSparkContext(): Unit = SparkContextHelper.forceStopActiveSparkContext()

  def createConf(appID: String = UUID.randomUUID().toString): SparkConf = {
    new SparkConf()
      .setMaster("local[*]")
      .setAppName("test")
      .set("spark.ui.enabled", "false")
      .set("spark.app.id", appID)
      .set("spark.driver.host", "localhost")
  }

  def createLocalSparkSession(appID: String = UUID.randomUUID().toString): SparkSession =
    SparkSession.builder().config(createConf(appID)).getOrCreate()

  def createLocalSparkContext(appID: String = UUID.randomUUID().toString): SparkContext = {
    // Stop the spark context if already running
    SparkContextHelper.forceStopActiveSparkContext()

    val tmpDirPath = Files
      .createTempDirectory("tmp_spark_checkpoint_dir")
      .toAbsolutePath

    sys.addShutdownHook {
      FileUtils.forceDeleteOnExit(new File(tmpDirPath.toUri))
    }

    createLocalSparkSession(appID).sparkContext.tap{ sc =>
      sc.setCheckpointDir(tmpDirPath.toString)
    }
  }
}