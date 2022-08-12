package com.github.anskarl.parsimonious.scrooge.flink

import com.github.anskarl.parsimonious.flink.FlinkEmbeddedClusterTestHelper
import com.github.anskarl.parsimonious.scrooge.DummyScroogeGenerators
import com.github.anskarl.parsimonious.scrooge.models.ComplexDummy
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.Checkers

class ScroogeSerializerPropertyTest extends AnyWordSpecLike
  with Matchers with Checkers with FlinkEmbeddedClusterTestHelper with DummyScroogeGenerators {

  "Scrooge serialization support for Flink - property test" should {
    "encode/decode Scrooge generated classes" in {
      val prop = forAll(Gen.nonEmptyListOf(arbComplexDummy.arbitrary)) { inputList: List[ComplexDummy] =>
        implicit val ti = ScroogeTypeInformation(classOf[ComplexDummy])
        val result = env.fromElements(inputList:_*)(ti).executeAndCollect().toList
        inputList == result
      }
      check(prop)
    }
  }

}
