package io.github.anskarl.parsimonious

import org.scalacheck.{Arbitrary, Gen}
//import scala.collection.JavaConverters._
import scala.jdk.CollectionConverters._
import scala.util.chaining._

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
    } yield new BasicDummy().tap{ d =>
      d.setReqStr(reqStr)
      d.setStr(str)
      d.setInt16(i16)
      d.setInt32(i32)
      d.setInt64(i64)
      d.setDbl(dbl)
      d.setBl(bl)
      d.setBin(bin)
    }
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
    } yield {
      val cd = new ComplexDummy()
      cd.setBdList(bdList.asJava)
      cd.setBdSet(bdSet.asJava)
      cd.setStrToBdMap(strToBdMap.asJava)
      cd.setBdToStrMap(bdToStrMap.asJava)
      cd.setEnumDummy(enum)
      cd.setUnionDummy(union)
      cd.setUnionRecursiveDummy(unionRecursive)
      cd
    }
  )
}
