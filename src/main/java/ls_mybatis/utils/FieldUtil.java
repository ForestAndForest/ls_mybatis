package ls_mybatis.utils;

import ls_mybatis.annotation.Column;
import ls_mybatis.core.SFunction;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author 29002
 */
public class FieldUtil {

    public static <T> String getField(SFunction<T, ?> fn, Class<T> clazz) {
        try {
            // 从function取出序列化方法
            Method writeReplaceMethod;
            writeReplaceMethod = fn.getClass().getDeclaredMethod("writeReplace");

            // 从序列化方法取出序列化的lambda信息
            writeReplaceMethod.setAccessible(true);
            SerializedLambda serializedLambda;

            serializedLambda = (SerializedLambda) writeReplaceMethod.invoke(fn);

            return getFieldName(serializedLambda, clazz);

        } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    private static String getFieldName(SerializedLambda serializedLambda, Class<?> clazz) throws NoSuchFieldException {
        String implMethodName = serializedLambda.getImplMethodName();
        // 确保方法是符合规范的get方法，boolean类型是is开头
        String fieldName = getFieldName(implMethodName);
        Field field = clazz.getDeclaredField(fieldName);
        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            String columnName = column.value();
            return columnName.isEmpty() ? fieldName : columnName;
        }
        return fieldName;
    }

    private static String getFieldName(String implMethodName) {
        if (!implMethodName.startsWith("is") && !implMethodName.startsWith("get")) {
            throw new RuntimeException("get方法名称: " + implMethodName + ", 不符合java bean规范");
        }

        // get方法开头为 is 或者 get，将方法名 去除is或者get，然后首字母小写，就是属性名
        int prefixLen = implMethodName.startsWith("is") ? 2 : 3;

        String fieldName = implMethodName.substring(prefixLen);
        String firstChar = fieldName.substring(0, 1);
        fieldName = fieldName.replaceFirst(firstChar, firstChar.toLowerCase());
        return fieldName;
    }
}
