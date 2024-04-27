package ls_mybatis.core;


import ls_mybatis.utils.FieldUtil;

import java.util.Objects;

public class QueryWrapper<T> {

    private final StringBuilder condition = new StringBuilder();

    public QueryWrapper<T> eq(SFunction<T, Object> field, Object value) {
        String fieldName = FieldUtil.getField(field);
        appendCondition(fieldName, "=", value);
        return this;
    }

    public QueryWrapper<T> gt(SFunction<T, Object> field, Object value) {
        String fieldName = FieldUtil.getField(field);
        appendCondition(fieldName, ">", value);
        return this;
    }

    public QueryWrapper<T> lt(SFunction<T, Object> field, Object value) {
        String fieldName = FieldUtil.getField(field);
        appendCondition(fieldName, "<", value);
        return this;
    }

    public QueryWrapper<T> like(SFunction<T, String> field, String value) {
        String fieldName = FieldUtil.getField(field);
        appendCondition(fieldName, "LIKE", "%" + Objects.requireNonNull(value) + "%");
        return this;
    }

    private void appendCondition(String fieldName, String operator, Object value) {
        if (value instanceof String) {
            value = "'" + value + "'";
        }
        condition.append(camelToSnakeCase(fieldName)).append(" ").append(operator).append(" ").append(value).append(" AND ");
    }

    public String build() {
        if (condition.isEmpty()) {
            return "1=1"; // 默认条件，避免空条件导致语法错误
        } else {
            // 去除末尾多余的 " AND "
            return condition.substring(0, condition.length() - 5);
        }
    }

    public static String camelToSnakeCase(String camelCaseString) {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (char character : camelCaseString.toCharArray()) {
            if (Character.isUpperCase(character)) {
                if (!first) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(character));
            } else {
                result.append(character);
            }
            first = false;
        }

        return result.toString();
    }


}
