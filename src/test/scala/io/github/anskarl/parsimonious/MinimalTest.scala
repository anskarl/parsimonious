package io.github.anskarl.parsimonious

import com.holdenkarau.spark.testing.DataFrameSuiteBase
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.types.StructType
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import io.github.anskarl.parsimonious.Converters._

class MinimalTest extends AnyWordSpecLike with DataFrameSuiteBase with Matchers {

  "Basic encode/decode functionality" should {
    "encode/decode Thrift class to Spark Rows" in {

      val sparkSchema: StructType = ThriftRowConverter.extractSchema(classOf[BasicDummy])

      val exampleData =
        for (index <- 1 to 100)
          yield new BasicDummy().setReqStr(s"index: ${index}").setInt32(index).setBl(index % 10 == 0)

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
