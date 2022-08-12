package com.github.anskarl.parsimonious.flink

import com.github.anskarl.parsimonious.DummyThriftGenerators
import com.github.anskarl.parsimonious.pojo.models._
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.Checkers

class ThriftSerializerPropertyTest extends AnyWordSpecLike
  with Matchers with Checkers with FlinkEmbeddedClusterTestHelper with DummyThriftGenerators {

  "Thrift serialization support for Flink - property test" should {
    "encode/decode Thrift generated classes" in {
      val prop = forAll(Gen.nonEmptyListOf(arbComplexDummy.arbitrary)) { inputList: List[ComplexDummy] =>
        implicit val ti: ThriftTypeInformation[ComplexDummy] = ThriftTypeInformation[ComplexDummy](classOf[ComplexDummy])
        val result = env.fromElements(inputList:_*)(ti).executeAndCollect().toList
        inputList == result
      }
      check(prop)
    }
  }

}
