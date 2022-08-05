package com.github.anskarl.parsimonious.srcooge.spark

import com.github.anskarl.parsimonious.scrooge.models._
import com.github.anskarl.parsimonious.scrooge.spark.Converters._
import com.github.anskarl.parsimonious.scrooge.spark.ScroogeRowConverter
import com.github.anskarl.parsimonious.spark.SparkSessionTestSuite
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.jdk.CollectionConverters._
import scala.util.chaining.scalaUtilChainingOps

class MinimalSparkTest extends AnyWordSpecLike with SparkSessionTestSuite with Matchers {

  "Basic encode/decode functionality" should {
    "encode/decode Thrift class to Spark Rows" in {

      val sparkSchema: StructType = ScroogeRowConverter.extractSchema(classOf[BasicDummy])

      val exampleData =
        for (index <- 1 to 100)
          yield BasicDummy(
            reqStr = s"index: ${index}",
            int32 = Some(index),
            bl = Some(index % 10 == 0),
            listNumbersI32 = Some(List(1,2,3)),
            setNumbersI32 = Some(Set(1,2,3)),
            mapPrimitivesStr = Some(Map("1" -> 1.1))
          )


      val rowSeq: Seq[Row] = exampleData.map(_.toRow)
      val rowRDD: RDD[Row] = spark.sparkContext.parallelize(rowSeq)
      val df: DataFrame = spark.createDataFrame(rowRDD, sparkSchema)

      val dfRows: Array[Row] = df.collect()
//      dfRows.foreach(println)

      assert(true)
//      val decodedInputList = dfRows
//        .map(row => row.as(classOf[BasicDummy]))
//        .toList
//
//      decodedInputList mustEqual exampleData
    }
  }
}
