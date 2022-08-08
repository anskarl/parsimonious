package com.github.anskarl.parsimonious.srcooge.spark

import com.github.anskarl.parsimonious.scrooge.models._
import com.github.anskarl.parsimonious.scrooge.{DummyScroogeGenerators, UnionBuilders}
import com.github.anskarl.parsimonious.scrooge.spark.{RowScroogeConverter, ScroogeRowConverter}
import com.github.anskarl.parsimonious.spark.SparkSessionTestSuite
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.Checkers
import com.github.anskarl.parsimonious.scrooge.spark.Converters._

import scala.util.chaining._

class ScroogeSparkConversionsTest extends AnyWordSpecLike with SparkSessionTestSuite with Matchers with Checkers with DummyScroogeGenerators {

  "Scrooge Row <> Thrift converters" should {
    "encode/decode Thrift generated classes to Spark Rows" in {
      val prop = forAll(Gen.nonEmptyListOf(arbComplexDummy.arbitrary)) { inputList =>

        val sparkSchema: StructType = ScroogeRowConverter.extractSchema(classOf[ComplexDummy])

        val rowSeq: Seq[Row] = inputList.map(ScroogeRowConverter.convert(_))
        val rowRDD: RDD[Row] = spark.sparkContext.parallelize(rowSeq)
        val df = spark.createDataFrame(rowRDD, sparkSchema)
        val dfRows = df.collect().toList

        implicit val unionBuilders: UnionBuilders = UnionBuilders.create(classOf[ComplexDummy])

        val decodedInputSet = dfRows
          .map(row => RowScroogeConverter.convert(classOf[ComplexDummy], row))
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
      val example = ComplexDummy(unionRecursiveDummy =
        Option(UnionRecursiveDummy.Ur(UnionRecursiveDummy.Ur(UnionRecursiveDummy.Bl(true))))
      )

      val inputList = List(example)

      val sparkSchema = ScroogeRowConverter.extractSchema(classOf[ComplexDummy])

      val rowSeq = inputList.map(ScroogeRowConverter.convert(_))
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
