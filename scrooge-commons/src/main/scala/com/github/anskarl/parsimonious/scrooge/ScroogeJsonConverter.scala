package com.github.anskarl.parsimonious.scrooge

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.anskarl.parsimonious.{ComplexDummy, UnionDummy, UnionRecursiveDummy}
import com.twitter.scrooge.{ThriftEnum, ThriftStruct, ThriftStructFieldInfo, ThriftUnion}
import org.apache.thrift.protocol.TType

import java.nio.ByteBuffer
import scala.collection.JavaConverters._
import scala.util.chaining.scalaUtilChainingOps

object ScroogeJsonConverter {
  private final val nodeFactory = mapper.getNodeFactory

  def convert[T <: ThriftStruct with Product](instance: T): ObjectNode ={
    val codec = com.twitter.scrooge.ThriftStructCodec.forStructClass(instance.getClass)
    val fieldInfos: Seq[ThriftStructFieldInfo] = getFieldInfos(codec)

    if(codec.metaData.unionFields.isEmpty) convertStruct(instance, fieldInfos)
    else convertUnion(instance.asInstanceOf[ThriftUnion])
  }

  def convertUnion[T <: ThriftUnion](instance: T): ObjectNode = {
    val fieldInfo = instance.unionStructFieldInfo.get
    mapper.createObjectNode().tap{ node =>
      convertScroogeElmToJSONElm(instance.containedValue(), fieldInfo)
        .foreach(jsonNode => node.replace(fieldInfo.tfield.name, jsonNode))
    }
  }

  def convertStruct[T <: ThriftStruct with Product](instance: T, fieldInfos: Seq[ThriftStructFieldInfo]): ObjectNode ={
    fieldInfos.zipWithIndex.foldLeft(mapper.createObjectNode()){
      case (node, (fieldInfo, index)) =>
        val elm = instance.productElement(index)
        val jsonNodeOpt = convertScroogeElmToJSONElm(elm, fieldInfo)
        jsonNodeOpt.foreach(jsonNode => node.replace(fieldInfo.tfield.name, jsonNode))
        node
    }
  }

  def decodeSingle[T](elm: Any, fieldInfo: ThriftStructFieldInfo): Option[T] =
    if(fieldInfo.isOptional) elm.asInstanceOf[Option[T]] else Option(elm.asInstanceOf[T])

  def convertScroogeElmToJSONElm(elm: Any, fieldInfo: ThriftStructFieldInfo): Option[JsonNode] = {
    val fieldType = fieldInfo.tfield.`type`

    fieldType match {
      case TType.BOOL =>
        decodeSingle[Boolean](elm, fieldInfo)
          .map(decodedEntry => nodeFactory.booleanNode(decodedEntry))

      case TType.BYTE =>
        decodeSingle[Byte](elm, fieldInfo)
          .map(decodedEntry => nodeFactory.numberNode(java.lang.Byte.valueOf(decodedEntry.byteValue()).intValue()))

      case TType.DOUBLE =>
        decodeSingle[Double](elm, fieldInfo)
          .map(decodedEntry => nodeFactory.numberNode(java.lang.Double.valueOf(decodedEntry.doubleValue())))

      case TType.I16 =>
        decodeSingle[Short](elm, fieldInfo)
          .map(decodedEntry => nodeFactory.numberNode(java.lang.Short.valueOf(decodedEntry.shortValue())))

      case TType.I32 =>
        decodeSingle[Int](elm, fieldInfo)
          .map(decodedEntry => nodeFactory.numberNode(java.lang.Integer.valueOf(decodedEntry.intValue())))

      case TType.I64 =>
        decodeSingle[Long](elm, fieldInfo)
          .map(decodedEntry => nodeFactory.numberNode(java.lang.Long.valueOf(decodedEntry.longValue())))

      case TType.STRING =>
        if(!classOf[String].equals(fieldInfo.manifest.runtimeClass))
          decodeSingle[ByteBuffer](elm, fieldInfo).map(b => nodeFactory.binaryNode(b.array()))
        else
          decodeSingle[String](elm, fieldInfo).map(decodedEntry => nodeFactory.textNode(decodedEntry))

      case TType.STRUCT =>
        if(fieldInfo.isOptional) elm.asInstanceOf[Option[ThriftStruct with Product]].map(convert)
        else Option(convert(elm.asInstanceOf[ThriftStruct with Product]))

      case TType.MAP =>

        val keyManifest = fieldInfo.keyManifest.get
        val keyThriftStructFieldInfo = getThriftStructFieldInfo(fieldInfo.tfield.name+"_key", keyManifest)
        val valueManifest = fieldInfo.valueManifest.get
        val valueThriftStructFieldInfo = getThriftStructFieldInfo(fieldInfo.tfield.name+"_value", valueManifest)


        if(fieldInfo.isOptional) elm.asInstanceOf[Option[_]].map(e => convertMap(keyThriftStructFieldInfo, valueThriftStructFieldInfo, e))
        else Option(convertMap(keyThriftStructFieldInfo, valueThriftStructFieldInfo, elm))

      case TType.LIST | TType.SET =>
        val valueManifest = fieldInfo.valueManifest.get
        val valueThriftStructFieldInfo = getThriftStructFieldInfo(fieldInfo.tfield.name+"_value", valueManifest)

        val elementsOpt =
          if(fieldInfo.isOptional)
            elm.asInstanceOf[Option[Iterable[Any]]].map(_.flatMap(e => convertScroogeElmToJSONElm(e,valueThriftStructFieldInfo)))
          else
            Option(elm.asInstanceOf[Iterable[Any]].flatMap(e => convertScroogeElmToJSONElm(e,valueThriftStructFieldInfo)))

        elementsOpt.map{ elements =>
          nodeFactory.arrayNode().addAll(elements.toSeq.asJava)
        }

      case TType.ENUM =>
        elm.asInstanceOf[Option[ThriftEnum]]
          .map(e => com.twitter.scrooge.ThriftEnumObject.forEnumClass(e.getClass).getOrUnknown(e.value).name)
          .map(nodeFactory.textNode)

      case illegalType @ _ => throw new IllegalArgumentException(s"Illegal Thrift type: $illegalType")

    }
  }

  private def convertMap(keyThriftStructFieldInfo: ThriftStructFieldInfo, valueThriftStructFieldInfo: ThriftStructFieldInfo, elm: Any): JsonNode ={
    val mapEntries = elm.asInstanceOf[Map[Any,Any]]

    val keyVals = mapEntries.map{ case (k, v) =>
      val kJson = convertScroogeElmToJSONElm(k, keyThriftStructFieldInfo).get
      val vJson = convertScroogeElmToJSONElm(v, valueThriftStructFieldInfo).get
      kJson -> vJson
    }
    // When the key is not STRING, we convert the map to a list of key/value struct
    if(keyThriftStructFieldInfo.tfield.`type`== TType.STRING){
      keyVals.foldLeft(nodeFactory.objectNode()){
        case (node, (k, v)) =>
          node.replace(k.asText(), v)
          node
      }
    }
    else {
      val elements = keyVals.map{case (k,v) =>
        val objNode = nodeFactory.objectNode()
        objNode.replace(keyName, k)
        objNode.replace(valName, v)
        objNode
      }.toSeq

      nodeFactory.arrayNode().addAll(elements.asJava)
    }
  }

}

object DeleteMeEnc extends App {

  import com.github.anskarl.parsimonious.{BasicDummy, EnumDummy, NestedDummy, PropertyValue}

  /*
  struct ComplexDummy {
    1: optional list<BasicDummy> bdList
    2: optional set<BasicDummy> bdSet
    3: optional map<string, BasicDummy> strToBdMap
    4: optional map<BasicDummy, string> bdToStrMap
    5: optional EnumDummy enumDummy
    6: optional UnionDummy unionDummy
    7: optional UnionRecursiveDummy unionRecursiveDummy
}
   */


//  val instance = BasicDummy(
//    reqStr = "req_str",
//    str = Some("opt_str"),
//    int16 = Some(16.toShort),
//    enm = Some(EnumDummy.No),
//    bl=Some(true),
////    listNumbersI32 = Some(List(1,2,3)),
//    listNumbersI32 = Some(List.empty),
//    listStruct = Some(Seq(PropertyValue("prop1", "val1"))),
//    listNumbersDouble= Some(List(1.1, 2.2, 3.3)),
//    setNumbersI32 = Some(Set(1,2,3,4,4,3,2,1)),
//    mapPrimitivesStr = Some(Map("a"-> 1.1, "b" -> 2.2)),
//    mapPrimitives = Some(Map(1 -> 1.1, 2-> 2.2)),
//    mapStructKey = Some(Map(
//      PropertyValue("a", "aa") -> 1,
//      PropertyValue("b", "bb") -> 2)
//    ),
//    bin = Some(ByteBuffer.wrap("foo".getBytes()))
//  )
//
//  val nestedInstance = NestedDummy("str", basic= instance, basicOpt =None)
//  val obj = ScroogeJsonConverter.convert(nestedInstance)
//
//  println(obj.toPrettyString)


  val instanceComplexDummy = ComplexDummy(
    bdList=Some(List()),
    bdSet = Some(Set()),
    strToBdMap = Some(Map()),
    bdToStrMap = Some(Map()),
    enumDummy = Some(EnumDummy.Maybe),
    unionDummy = Some(UnionDummy.Str("aaa")),
    unionRecursiveDummy = Some(UnionRecursiveDummy.Ur(ur = UnionRecursiveDummy.Ur(ur =UnionRecursiveDummy.Bl(bl = true))))
  )
//  val ser = com.twitter.scrooge.JsonThriftSerializer(UnionRecursiveDummy)
//  println(ser.toString(UnionRecursiveDummy.Ur(ur = UnionRecursiveDummy.Ur(ur =UnionRecursiveDummy.Bl(bl = true)))))
//
//  println(UnionRecursiveDummy.Ur(ur = UnionRecursiveDummy.Ur(ur =UnionRecursiveDummy.Bl(bl = true))))

  val objComplexDummy = ScroogeJsonConverter.convert(instanceComplexDummy)
  println(objComplexDummy.toPrettyString)
}
