package com.github.anskarl.parsimonious.spark

import com.github.anskarl.parsimonious.pojo.models._
import com.github.anskarl.parsimonious.spark.Converters._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.chaining.scalaUtilChainingOps
import scala.jdk.CollectionConverters._

class ThriftMinimalSparkTest extends AnyWordSpecLike with SparkSessionTestSuite with Matchers {

  "Basic encode/decode functionality" should {
    "encode/decode Thrift class to Spark Rows" in {

      val sparkSchema: StructType = ThriftRowConverter.extractSchema(classOf[BasicDummy])

      val exampleData =
        for (index <- 1 to 100)
          yield new BasicDummy().tap{ d =>
            d.setReqStr(s"index: ${index}")
            d.setInt32(index)
            d.setBl(index % 10 == 0)
            d.setListNumbersI32(List(1,2,3).map(java.lang.Integer.valueOf).asJava)
            d.setSetNumbersI32(Set(1,2,3).map(java.lang.Integer.valueOf).asJava)
            d.setMapPrimitivesStr(Map("1" -> java.lang.Double.valueOf(1.1)).asJava)
          }

      val rowSeq: Seq[Row] = exampleData.map(_.toRow)
      val rowRDD: RDD[Row] = spark.sparkContext.parallelize(rowSeq)
      val df: DataFrame = spark.createDataFrame(rowRDD, sparkSchema)

      val dfRows: Array[Row] = df.collect()

      val decodedInputList = dfRows
        .map(row => row.as(classOf[BasicDummy]))
        .toList

      decodedInputList mustEqual exampleData
    }
  }
}
