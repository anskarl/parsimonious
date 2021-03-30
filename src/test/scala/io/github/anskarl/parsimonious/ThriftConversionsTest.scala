package io.github.anskarl.parsimonious

import com.holdenkarau.spark.testing.DataFrameSuiteBase
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.Checkers
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalacheck.Prop.forAll

import scala.collection.JavaConverters._
import scala.util.chaining._

class ThriftConversionsTest extends AnyWordSpecLike with DataFrameSuiteBase with Matchers with Checkers {


  implicit val arbBasicDummy: Arbitrary[BasicDummy] = Arbitrary(
    for {
      reqStr <- Arbitrary.arbitrary[String]
      str <- Arbitrary.arbitrary[String]
      i16 <- Arbitrary.arbitrary[Short]
      i32 <- Arbitrary.arbitrary[Int]
      i64 <- Arbitrary.arbitrary[Long]
      dbl <- Arbitrary.arbitrary[Double]
      bl <- Arbitrary.arbitrary[Boolean]
      bin <- Arbitrary.arbitrary[Array[Byte]]
    } yield new BasicDummy()
      .setReqStr(reqStr)
      .setStr(str)
      .setInt16(i16)
      .setInt32(i32)
      .setInt64(i64)
      .setDbl(dbl)
      .setBl(bl)
      .setBin(bin)
  )

  implicit val arbUnionDummy: Arbitrary[UnionDummy] = Arbitrary(Gen.lzy(Gen.oneOf(
    Arbitrary.arbitrary[Double].map(UnionDummy.dbl),
    Arbitrary.arbitrary[String].map(UnionDummy.str)
  )))

  implicit val arbEnumDummy: Arbitrary[EnumDummy] = Arbitrary(
    for {
      enumVal <- Gen.choose[Int](0, EnumDummy.values().length - 1)
    } yield EnumDummy.findByValue(enumVal)
  )

  // Recursive Generator
  val baseCaseUnionRecursive: Gen[UnionRecursiveDummy] = Arbitrary.arbitrary[Boolean].map(UnionRecursiveDummy.bl)
  val unionRecursiveGen: Gen[UnionRecursiveDummy] = Gen.lzy(Gen.oneOf(baseCaseUnionRecursive, recurCaseUnionRecursive))
  val recurCaseUnionRecursive: Gen[UnionRecursiveDummy] = unionRecursiveGen.map(UnionRecursiveDummy.ur)

  implicit val arbUnionRecursive: Arbitrary[UnionRecursiveDummy] = Arbitrary(unionRecursiveGen)

  implicit val arbComplexDummy: Arbitrary[ComplexDummy] = Arbitrary(
    for {
      bdList <- Arbitrary.arbitrary[List[BasicDummy]]
      bdSet <- Arbitrary.arbitrary[Set[BasicDummy]]
      strToBdMap <- Arbitrary.arbitrary[Map[String, BasicDummy]]
      bdToStrMap <- Arbitrary.arbitrary[Map[BasicDummy, String]]
      enum <- Arbitrary.arbitrary[EnumDummy]
      union <- Arbitrary.arbitrary[UnionDummy]
      unionRecursive <- Arbitrary.arbitrary[UnionRecursiveDummy]
    } yield new ComplexDummy()
      .setBdList(bdList.asJava)
      .setBdSet(bdSet.asJava)
      .setStrToBdMap(strToBdMap.asJava)
      .setBdToStrMap(bdToStrMap.asJava)
      .setEnumDummy(enum)
      .setUnionDummy(union)
      .setUnionRecursiveDummy(unionRecursive)
  )

  "Row <> Thrift converters" should {
    "encode/decode Thrift generated classes to Spark Rows" in {
      val prop = forAll(Gen.nonEmptyListOf(arbComplexDummy.arbitrary)) { inputList: List[ComplexDummy] =>

        val sparkSchema: StructType = ThriftRowConverter.extractSchema(classOf[ComplexDummy])

        val rowSeq: Seq[Row] = inputList.map(ThriftRowConverter.convert(_))
        val rowRDD: RDD[Row] = spark.sparkContext.parallelize(rowSeq)
        val df = spark.createDataFrame(rowRDD, sparkSchema)
        val dfRows = df.collect()

        val decodedInputList = dfRows
          .map(RowThriftConverter.convert(classOf[ComplexDummy], _))
          .toList

        inputList.toSet == decodedInputList.toSet
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
      import Converters._
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
