package com.github.anskarl.parsimonious.scrooge.json

import com.github.anskarl.parsimonious.scrooge.{DummyGenerators, JsonScroogeConverter, ScroogeJsonConverter}
import com.github.anskarl.parsimonious.{ComplexDummy, UnionRecursiveDummy}
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.Checkers

class ScroogeJsonConversionsTest extends AnyWordSpecLike with Matchers with Checkers with DummyGenerators {

  "Json <> Scrooge converters" should {
    "encode/decode Scrooge Thrift generated classes to Json" in {
      val prop = forAll(Gen.nonEmptyListOf(arbComplexDummy.arbitrary)) { inputList: List[ComplexDummy] =>

        val encoded = inputList.map(cd => ScroogeJsonConverter.convert(cd))
        val decoded = encoded.map{ node =>
          JsonScroogeConverter.convert(classOf[ComplexDummy], node)
        }

        inputList.toSet == decoded.toSet
      }
      check(prop)
    }

    "support for recursive schema" in {
      val example = ComplexDummy(unionRecursiveDummy = Some(UnionRecursiveDummy.Ur(UnionRecursiveDummy.Ur(UnionRecursiveDummy.Bl(true)))))

      val jsonNode = ScroogeJsonConverter.convert(example)
      val decoded = JsonScroogeConverter.convert(classOf[ComplexDummy], jsonNode)

      example mustEqual decoded

    }
  }


}
