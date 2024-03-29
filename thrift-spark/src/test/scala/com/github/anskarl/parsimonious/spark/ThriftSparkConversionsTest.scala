package com.github.anskarl.parsimonious.spark

import com.github.anskarl.parsimonious.pojo.models._
import com.github.anskarl.parsimonious.DummyThriftGenerators
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.scalacheck.Prop.forAll
import org.scalacheck.Gen
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.Checkers

import scala.util.chaining._

class ThriftSparkConversionsTest extends AnyWordSpecLike with SparkSessionTestSuite with Matchers with Checkers with DummyThriftGenerators {

  "Thrift Row <> Thrift converters" should {
    "encode/decode Thrift generated classes to Spark Rows" in {
      val prop = forAll(Gen.nonEmptyListOf(arbComplexDummy.arbitrary)) { inputList =>

        val sparkSchema: StructType = ThriftRowConverter.extractSchema(classOf[ComplexDummy])

        val rowSeq: Seq[Row] = inputList.map(ThriftRowConverter.convert(_))
        val rowRDD: RDD[Row] = spark.sparkContext.parallelize(rowSeq)
        val df = spark.createDataFrame(rowRDD, sparkSchema)
        val dfRows = df.collect().toList


        val decodedInputSet = dfRows
          .map(RowThriftConverter.convert(classOf[ComplexDummy], _))
          .toSet

        inputList.toSet == decodedInputSet
      }

      check(prop)
    }

    // Schema in Spark Dataframes/Datasets cannot support recursive structures.
    //
    // In Spark StructType represents the schema tree and should be completely
    // defined, therefore it cannot support structures with recursion and at the
    // same time contain schema definitions of any recursive depth.
    //
    // As a result, the recursive struct is being handled as binary type.
    "support for recursive schema" in {
      import com.github.anskarl.parsimonious.spark.Converters._
      val example = new ComplexDummy().tap{ d =>
        d.setUnionRecursiveDummy(
          new UnionRecursiveDummy().tap{ u1 =>
            u1.setUr(new UnionRecursiveDummy().tap(u2 => u2.setBl(true)))
          }
        )
      }

      val inputList = List(example)

      val sparkSchema = ThriftRowConverter.extractSchema(classOf[ComplexDummy])

      val rowSeq = inputList.map(_.toRow)
      val rowRDD = spark.sparkContext.parallelize(rowSeq)
      val df = spark.createDataFrame(rowRDD, sparkSchema)

      val dfRows = df.collect()

      val decodedInput = dfRows
        .map(_.as(classOf[ComplexDummy]))
        .head

      example mustEqual decodedInput
    }
  }
}
