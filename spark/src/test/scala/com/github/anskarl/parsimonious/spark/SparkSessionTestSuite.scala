package com.github.anskarl.parsimonious.spark


import org.apache.spark.sql.SparkSession
import org.scalatest.{ BeforeAndAfterAll, Informing, Suite }

import scala.util.control.NonFatal

trait SparkSessionTestSuite extends BeforeAndAfterAll with SparkSessionTestSuiteLike {
  self: Suite with Informing =>

  override def beforeAll(): Unit = {
    this.startSparkSession()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    this.stopSparkSession()
    super.afterAll()
  }

}

trait SparkSessionTestSuiteLike {
  self: Suite with Informing =>

  @transient private var _ss: SparkSession = _

  def spark: SparkSession = _ss

  def startSparkSession(): Unit = {
    try {
      val appID = this.getClass.getName + "_" + math.floor(math.random * 10E5).toLong
      _ss = SparkHelper.createLocalSparkSession(appID)
    } catch {
      case NonFatal(ex) => fail(ex)
    }
  }

  def stopSparkSession(): Unit = {
    if (_ss != null) _ss.stop()
    _ss = null
  }
}
