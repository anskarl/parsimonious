package io.github.anskarl.parsimonious

import com.holdenkarau.spark.testing.DataFrameSuiteBase
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.JavaConverters._

class MinimalTest extends AnyWordSpecLike with DataFrameSuiteBase with Matchers {

  "Basic encode/decode functionality" should {
    "encode/decode Thrift class to Spark Rows" in {

      val sparkSchema: StructType = ThriftRowConverter.extractSchema(classOf[ComplexDummy])
      val basicDummy = new BasicDummy().setReqStr("req").setInt32(42)
      val complexDummy = new ComplexDummy().setBdList(List(basicDummy).asJava)

      val inputList = List(complexDummy, complexDummy)


      val rowSeq: Seq[Row] = inputList.map(ThriftRowConverter.convert(_))
      val rowRDD: RDD[Row] = spark.sparkContext.parallelize(rowSeq)
      val df = spark.createDataFrame(rowRDD, sparkSchema)
      val dfRows = df.collect()

      val decodedInputList = dfRows
        .map(RowThriftConverter.convert(classOf[ComplexDummy], _))
        .toList

      decodedInputList mustEqual inputList
    }
  }
}
