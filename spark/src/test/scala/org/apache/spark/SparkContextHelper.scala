package org.apache.spark

object SparkContextHelper {
  def forceStopActiveSparkContext(): Unit = {
    SparkContext.getActive.foreach(_.stop())
  }
}
