package ls_mybatis.core;


import ls_mybatis.utils.FieldUtil;

import java.util.Objects;

@SuppressWarnings("all")
public class QueryWrapper<T> {

    private final StringBuilder condition = new StringBuilder();
    private StringBuilder orderBy = new StringBuilder();
    private StringBuilder groupBy = new StringBuilder();
    private StringBuilder limit = new StringBuilder();
    private final StringBuilder having = new StringBuilder();
    private final Class<T> clazz;

    public QueryWrapper(Class<T> clazz) {
        this.clazz = clazz;
    }

    public QueryWrapper<T> orderBy(SFunction<T, Object> field, boolean isAsc) {
        String fieldName = FieldUtil.getField(field, clazz);
        orderBy =  new StringBuilder().append(" ORDER BY ").append(camelToSnakeCase(fieldName)).append(" ").append(isAsc ? "ASC" : "DESC");
        return this;
    }

    public QueryWrapper<T> groupBy(SFunction<T, Object> field) {
        String fieldName = FieldUtil.getField(field, clazz);
        groupBy = new StringBuilder().append(" GROUP BY (").append(camelToSnakeCase(fieldName)).append(")");
        return this;
    }

    public QueryWrapper<T> limit(int offset, int limit) {
        this.limit = new StringBuilder().append(" LIMIT ").append(offset).append(",").append(limit);
        return this;
    }

//    public QueryWrapper<T> having(SFunction<T, Object> field, Object value) {
//        String fieldName = FieldUtil.getField(field);
//        having.append("HAVING ").append(fieldName).append(operator).append(value);
//    }

    public QueryWrapper<T> eq(SFunction<T, Object> field, Object value) {
        System.out.println(field);
        String fieldName = FieldUtil.getField(field, clazz);
        appendCondition(fieldName, "=", value);
        return this;
    }

    public QueryWrapper<T> gt(SFunction<T, Object> field, Object value) {
        String fieldName = FieldUtil.getField(field, clazz);
        appendCondition(fieldName, ">", value);
        return this;
    }

    public QueryWrapper<T> lt(SFunction<T, Object> field, Object value) {
        String fieldName = FieldUtil.getField(field, clazz);
        appendCondition(fieldName, "<", value);
        return this;
    }

    public QueryWrapper<T> like(SFunction<T, String> field, String value) {
        String fieldName = FieldUtil.getField(field, clazz);
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
        if (condition.isEmpty() && orderBy.isEmpty() && groupBy.isEmpty() && limit.isEmpty()) {
            return ""; // 空条件
        } else {
            condition.insert(0, " WHERE ");
            if (!condition.isEmpty()) {
                // 去除末尾多余的 " AND "
                condition.delete(condition.length() - 5, condition.length());
            }
            return condition
                    .append(groupBy)
                    .append(orderBy)
                    .append(limit)
                    .toString();
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

    public StringBuilder getOrderBy() {
        return orderBy;
    }

    public StringBuilder getGroupBy() {
        return groupBy;
    }

    public StringBuilder getLimit() {
        return limit;
    }
}
