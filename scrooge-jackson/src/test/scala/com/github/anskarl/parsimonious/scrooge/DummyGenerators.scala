package com.github.anskarl.parsimonious.scrooge

import com.github.anskarl.parsimonious.{BasicDummy, ComplexDummy, EnumDummy, UnionDummy, UnionRecursiveDummy}
import org.scalacheck.{Arbitrary, Gen}

import java.nio.ByteBuffer
import scala.jdk.CollectionConverters._

trait DummyGenerators {
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
    } yield BasicDummy(
      reqStr = reqStr,
      str = Option(str),
      int16 = Option(i16),
      int32 = Option(i32),
      int64 = Option(i64),
      dbl = Option(dbl),
      bl = Option(bl),
      bin = Option(ByteBuffer.wrap(bin))
    )
  )

  implicit val arbUnionDummy: Arbitrary[UnionDummy] = Arbitrary(Gen.lzy(Gen.oneOf(
    Arbitrary.arbitrary[Double].map(UnionDummy.Dbl),
    Arbitrary.arbitrary[String].map(UnionDummy.Str)
  )))

  implicit val arbEnumDummy: Arbitrary[EnumDummy] = Arbitrary(
    for {
      enumVal <- Gen.choose[Int](0, EnumDummy.list.length - 1)
    } yield EnumDummy(enumVal)
  )

  // Recursive Generator
  val baseCaseUnionRecursive: Gen[UnionRecursiveDummy] = Arbitrary.arbitrary[Boolean].map(UnionRecursiveDummy.Bl)
  val unionRecursiveGen: Gen[UnionRecursiveDummy] = Gen.lzy(Gen.oneOf(baseCaseUnionRecursive, recurCaseUnionRecursive))
  val recurCaseUnionRecursive: Gen[UnionRecursiveDummy] = unionRecursiveGen.map(UnionRecursiveDummy.Ur)

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
    } yield {
      ComplexDummy(
        bdList = Option(bdList),
        bdSet = Option(bdSet),
        strToBdMap = Option(strToBdMap),
        bdToStrMap = Option(bdToStrMap),
        enumDummy = Option(enum),
        unionDummy = Option(union),
        unionRecursiveDummy = Option(unionRecursive))
    }
  )
}
