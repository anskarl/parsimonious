package com.github.anskarl.parsimonious.spark

import com.github.anskarl.parsimonious.pojo.models._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class SimpleA extends AnyWordSpecLike  with SparkSessionTestSuite with Matchers {
  //  val schema = ThriftRowConverter.extractSchema(classOf[ComplexDummy])
  //  println(schema.treeString)

  "BB Basic encode/decode functionality" should {
    "encode/decode Thrift class to Spark Rows" in {
      val instance = new BasicDummy().setReqStr("req str")

      val schema = ThriftRowConverter.extractSchema(classOf[BasicDummy])
      val row = ThriftRowConverter.convert(instance)

      val rdd = spark.sparkContext.parallelize(Seq(row))
      val df = spark.createDataFrame(rdd, schema)
      df.show()
      assert(true)
    }
  }
}
