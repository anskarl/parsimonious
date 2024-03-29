package com.github.anskarl.parsimonious.json

import com.github.anskarl.parsimonious.pojo.models._
import com.github.anskarl.parsimonious.DummyThriftGenerators
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.Checkers
import com.github.anskarl.parsimonious.json.Converters._
import org.apache.thrift.TBase

import scala.util.chaining._

class ThriftJsonConversionsTest extends AnyWordSpecLike with Matchers with Checkers with DummyThriftGenerators {

  "Json <> Thrift converters" should {
    "encode/decode Thrift generated classes to Json" in {
      val prop = forAll(Gen.nonEmptyListOf(arbComplexDummy.arbitrary)) { inputList: List[ComplexDummy] =>

        val encoded = inputList.map(cd => ThriftJsonConverter.convert(cd.asInstanceOf[TBase[_,_]]).jsonNode.toString)
        val decoded = encoded.map(node => node.as(classOf[ComplexDummy]))

        inputList.toSet == decoded.toSet

      }
      check(prop)
    }

    "support for recursive schema" in {
      val example = new ComplexDummy().tap{ d =>
        d.setUnionRecursiveDummy(
          new UnionRecursiveDummy().tap{ u1 =>
            u1.setUr(new UnionRecursiveDummy().tap(u2 => u2.setBl(true)))
          }
        )
      }

      val jsonNode = example.toJsonNode
      val decoded = jsonNode.as(classOf[ComplexDummy])

      example mustEqual decoded

    }
  }


}
