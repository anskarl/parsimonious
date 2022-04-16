package io.github.anskarl.parsimonious;

import java.util.Map;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.meta_data.EnumMetaData;
import org.apache.thrift.meta_data.FieldMetaData;
import org.apache.thrift.meta_data.FieldValueMetaData;
import scala.collection.convert.WrapAsScala$;

/**
 * Some unsafe operations expressed in Java to avoid using `scala.language.existentials` in Scala.
 */
@SuppressWarnings("unchecked")
public final class UnsafeThriftHelpers {

    public static <T> scala.collection.mutable.Map<TFieldIdEnum, FieldMetaData> getStructMetaDataMap(Class<T> obj) {
        final Class<? extends TBase<? extends TBase<?, ?>, ? extends TFieldIdEnum>> aClass =
                (Class<? extends TBase<? extends TBase<?,?>, ? extends TFieldIdEnum>>) obj;

        final Map<TFieldIdEnum, FieldMetaData> structMetaDataMap =
                (Map<TFieldIdEnum, FieldMetaData>) FieldMetaData.getStructMetaDataMap(aClass);

        return WrapAsScala$.MODULE$.mapAsScalaMap(structMetaDataMap);
        //return JavaConverters.mapAsScalaMap(structMetaDataMap); //todo

    }

    public static <T extends Enum<T>> T enumOf(FieldValueMetaData meta, String elm) {

        final EnumMetaData metaData = (EnumMetaData) meta;
        final Class<T> enumClass = (Class<T>) metaData.enumClass;

        return Enum.valueOf(enumClass, elm);
    }

}
