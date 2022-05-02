package com.github.anskarl.parsimonious.scrooge

import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec, ThriftStructFieldInfo}

import scala.reflect.runtime.{universe => ru}
import com.twitter.util.reflect.Types._
import org.apache.thrift.protocol.TType

case class UnionBuilder[T <: ThriftStruct](
  name: String,
  className: String,
  unionFieldByNameBuilderMap: Map[String, Any => T]
)

case class UnionBuilders(
  builders: Map[String, UnionBuilder[_ <: ThriftStruct]]
){
  def build[T <: ThriftStruct](codec: ThriftStructCodec[T], fieldName: String, element: Any): T =
    this.build(codec.metaData.structClassName, fieldName, element)

  def build[T <: ThriftStruct](unionClassName: String, fieldName: String, element: Any): T =
    builders(unionClassName).unionFieldByNameBuilderMap(fieldName)(element).asInstanceOf[T]
}

object UnionBuilders {
  private final val mirror = ru.runtimeMirror(getClass.getClassLoader)
  private type UnionBuilderOpt = Option[UnionBuilder[_ <: ThriftStruct]]

  def create[T <: ThriftStruct: ru.TypeTag](structClass: Class[T]): UnionBuilders = UnionBuilders(
    extractThriftStruct(structClass)
      .flatMap( entry => entry.map(builder => builder.className -> builder) )
      .toMap
  )

  private[scrooge] def extractThriftStruct[T <: ThriftStruct: ru.TypeTag](structClass: Class[T]): Seq[Option[UnionBuilder[_ <: ThriftStruct]]] ={
    val codec: ThriftStructCodec[T] = com.twitter.scrooge.ThriftStructCodec.forStructClass(structClass)
    val result =
      if(codec.metaData.unionFields.nonEmpty) UnionBuilders.createBuilders(codec)
      else
        codec.metaData.fieldInfos.flatMap(extract)
    result
  }

  private[scrooge] def extract(fieldInfo: ThriftStructFieldInfo): Seq[Option[UnionBuilder[_ <: ThriftStruct]]] =
    fieldInfo.tfield.`type` match {
      case TType.STRUCT =>
        val valueStructClass = fieldInfo.manifest.runtimeClass.asInstanceOf[ThriftStructWithProduct]
        UnionBuilders.extractThriftStruct(valueStructClass)
      case TType.MAP =>
        val keyManifest = fieldInfo.keyManifest.get
        val keyThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_key", keyManifest)
        val valueManifest = fieldInfo.valueManifest.get
        val valueThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_value", valueManifest)
        extract(keyThriftStructFieldInfo) ++ extract(valueThriftStructFieldInfo)
      case TType.SET | TType.LIST =>
        val valueManifest = fieldInfo.valueManifest.get
        val valueThriftStructFieldInfo = ScroogeHelpers.getThriftStructFieldInfo(fieldInfo.tfield.name+"_values", valueManifest)
        extract(valueThriftStructFieldInfo)
      case _ => Seq[UnionBuilderOpt](None)
    }



  private[scrooge] def createBuilders[T <: ThriftStruct: ru.TypeTag](codec: ThriftStructCodec[T]): Seq[Option[UnionBuilder[_ <: ThriftStruct]]] ={
    val unionFields = codec.metaData.unionFields
    val structClassName = codec.metaData.structClassName

    if(unionFields.isEmpty) Seq[UnionBuilderOpt](None)
    else {
      var builderMap = Map.empty[String, Any => T]
      var result = Seq.empty[Option[UnionBuilder[_ <: ThriftStruct]]]

      for(unionFieldInfo <- unionFields) {
        val fieldClassName = unionFieldInfo.structFieldInfo.manifest.runtimeClass.getName

        // avoid infinite recursion, when having recursive structs/unions
        if (structClassName != fieldClassName)
          result ++= extract(unionFieldInfo.structFieldInfo)

        val name = unionFieldInfo.structFieldInfo.tfield.name
        val unionRuntimeClass = unionFieldInfo.fieldClassTag.runtimeClass
        val unionClassType: ru.Type = ru.typeOf(asTypeTag(unionRuntimeClass))

        val classUnion = unionClassType.typeSymbol.asClass
        val cu = mirror.reflectClass(classUnion)

        val ctor = unionClassType.decl(ru.termNames.CONSTRUCTOR).asMethod
        val ctorm = cu.reflectConstructor(ctor)

        val fun: Any => T = (element: Any ) => ctorm(element).asInstanceOf[T]
        builderMap += name -> fun
      }

      result ++= Seq(Some(
        UnionBuilder(
          name = codec.metaData.structName,
          className = codec.metaData.structClassName,
          unionFieldByNameBuilderMap = builderMap
        )
      ))

      result
    }
  }
}
