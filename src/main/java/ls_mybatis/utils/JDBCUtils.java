package ls_mybatis.utils;


import ls_mybatis.annotation.Column;
import ls_mybatis.annotation.Exclude;
import ls_mybatis.annotation.Id;
import ls_mybatis.annotation.Table;
import ls_mybatis.core.QueryWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.alibaba.fastjson2.util.TypeUtils.toDate;

/**
 * @author 29002
 * @version v0.1
 */
//@SuppressWarnings("all")
public class JDBCUtils {
    private static final Logger log = Logger.getLogger(JDBCUtils.class.getName());
    private static final Connection connection;
    private static boolean logging = false;

    // 在类加载时进行初始化
    static {
        System.out.println("---------------ls-mybatis v0.1---------------");
        System.out.println("---------------author:29002---------------");
        connection = setupConnection();
    }

    private static void log(String message) {
        if (logging) {
            log.log(Level.INFO, message);
        }
    }

    // 初始化数据库连接
    private static Connection setupConnection() {
        try {
            Properties properties = new Properties();
            InputStream inputStream = JDBCUtils.class.getClassLoader().getResourceAsStream("application.properties");
            properties.load(inputStream);
            String url = properties.getProperty("db.url");
            String username = properties.getProperty("db.username");
            String password = properties.getProperty("db.password");
            logging = Boolean.parseBoolean(properties.getProperty("db.logging"));
            // 自动注册 MySQL 驱动程序
            Class.forName(properties.getProperty("db.driver"));
            return DriverManager.getConnection(url, username, password);
        } catch (IOException e) {
            throw new RuntimeException("无法读取application.properties文件", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("找不到MySQL驱动程序", e);
        } catch (SQLException e) {
            throw new RuntimeException("无法建立数据库连接", e);
        }
    }


    // 获取数据库连接
    public static Connection getConnection() {
        return connection;
    }

    // 执行 SQL 查询并返回结果集
    public static ResultSet executeQuery(String sql) {
        try {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (logging) {
                log.log(Level.INFO, "执行 SQL: " + sql);
            }
            return rs;
        } catch (SQLException e) {
            throw new RuntimeException("无法执行 SQL 查询: " + sql, e);
        }
    }


    /**
     * 查询所有记录
     *
     * @param clazz 实体类的 Class 对象
     * @param <T>   实体类的类型
     * @return 包含所有记录的列表
     */
    public static <T> List<T> select(Class<T> clazz) {
        return select(clazz, new QueryWrapper<>(clazz));
    }

    /**
     * 根据查询条件返回符合条件的记录
     *
     * @param clazz        实体类的 Class 对象
     * @param queryWrapper 查询条件的封装对象
     * @param <T>          实体类的类型
     * @return 包含符合条件的记录的列表
     */
    public static <T> List<T> select(Class<T> clazz, QueryWrapper<T> queryWrapper) {
        List<T> result = new ArrayList<>();
        try {
            String tableName = getTableName(clazz);

            if (!queryWrapper.getGroupBy().isEmpty()) {
                throw new IllegalArgumentException("请使用 JDBCUtils.countMap（Class<T> clazz，QueryWrapper<T> queryWrapper） 方法进行分组查询");
            }

            // 构建 SQL 查询语句
            String sql = "SELECT * FROM " + tableName + queryWrapper.build();
            ResultSet rs = executeQuery(sql);

            // 遍历结果集, 将结果集转换为对象列表 反射机制
            while (rs != null && rs.next()) {
                T obj = mapResultSetToEntity(rs, clazz);
                result.add(obj);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("无法执行查询", e);
        }
    }

    /**
     * 返回符合条件的第一条记录
     *
     * @param clazz        实体类的 Class 对象
     * @param queryWrapper 查询条件的封装对象
     * @param <T>          实体类的类型
     * @return 符合条件的第一条记录，如果没有符合条件的记录则返回 null
     */
    public static <T> T selectOne(Class<T> clazz, QueryWrapper<T> queryWrapper) {
        StringBuilder limit = queryWrapper.getLimit();
        if (limit.isEmpty()) {
            queryWrapper.limit(0, 1);
        }
        List<T> users = select(clazz, queryWrapper);
        if (users == null || users.isEmpty()) {
            return null;
        }
        return users.get(0);
    }


    /**
     * 返回符合条件的记录数的映射
     *
     * @param clazz        实体类的 Class 对象
     * @param queryWrapper 查询条件的封装对象
     * @param <T>          实体类的类型
     * @return 符合条件的记录数的映射
     */
    public static <T> Map<Object, Integer> countMap(Class<T> clazz, QueryWrapper<T> queryWrapper) {
        Connection conn = getConnection();
        try (PreparedStatement pstmt = buildCountStatement(conn, clazz, queryWrapper)) {
            ResultSet rs = pstmt.executeQuery();
            return mapResultSetToCountMap(rs, queryWrapper);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 构建计数 SQL 查询语句的 PreparedStatement
    private static <T> PreparedStatement buildCountStatement(Connection conn, Class<T> clazz, QueryWrapper<T> queryWrapper) throws SQLException {
        String tableName = getTableName(clazz);
        StringBuilder groupBy = queryWrapper.getGroupBy();
        if (groupBy.isEmpty()) {
            throw new RuntimeException("没有找到分组条件\n修复建议:使用QueryWrapper.group()添加条件");
        }
        String groupByColumn = groupBy.substring(groupBy.indexOf("(") + 1, groupBy.indexOf(")"));
        String sql = "SELECT " + groupByColumn + " , count(*) as count FROM " + tableName + queryWrapper.build();
        return conn.prepareStatement(sql);
    }

    // 将结果集映射为计数 Map
    private static Map<Object, Integer> mapResultSetToCountMap(ResultSet rs, QueryWrapper<?> queryWrapper) throws SQLException {
        Map<Object, Integer> countMap = new HashMap<>();
        StringBuilder groupBy = queryWrapper.getGroupBy();
        String groupByColumn = groupBy.substring(groupBy.indexOf("(") + 1, groupBy.indexOf(")"));
        while (rs.next()) {
            Object groupByValue = rs.getObject(camelToSnakeCase(groupByColumn));
            int count = rs.getInt(camelToSnakeCase("count"));
            countMap.put(groupByValue, count);
        }
        return countMap;
    }

    /**
     * 更新数据库记录
     *
     * @param obj 包含更新数据的对象
     * @param <T> 对象的类型
     * @return 受影响的记录数
     */
    public static <T> int update(T obj) {
        Connection conn = getConnection();
        try {
            Class<?> clazz = obj.getClass();
            String tableName = getTableName(clazz);
            String primaryKey = getPrimaryKeyFieldName(clazz);
            StringBuilder sqlBuilder = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
            StringJoiner columnValues = new StringJoiner(", ");
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Exclude.class)) {
                    continue;
                }
                String columnName = getColumnName(field);
                columnValues.add(columnName + " = ?");
            }
            if (primaryKey.isEmpty()) {
                throw new RuntimeException("实体类" + clazz.getName() + "没有指定主键");
            }
            sqlBuilder.append(columnValues).append(" WHERE ").append(primaryKey).append(" = ?");
            String sql = sqlBuilder.toString();
            System.out.println(sql);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int parameterIndex = 1;
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Exclude.class)) {
                        continue;
                    }
                    field.setAccessible(true);
                    pstmt.setObject(parameterIndex++, field.get(obj));
                }
                pstmt.setObject(parameterIndex, getPrimaryKeyValue(obj));
                if (logging) {
                    log.log(Level.INFO, "执行SQL: " + pstmt);
                }
                return pstmt.executeUpdate();
            }
        } catch (SQLException | IllegalAccessException e) {
            log(e.getMessage());
            return 0;
        }
    }


    /**
     * 删除数据库记录
     *
     * @param clazz        实体类的 Class 对象
     * @param queryWrapper 查询条件的封装对象
     * @param <T>          实体类的类型
     * @return 受影响的记录数
     */
    public static <T> int delete(Class<?> clazz, QueryWrapper<T> queryWrapper) {
        Connection conn = getConnection();
        try {
            String tableName = getTableName(clazz);
            String condition = queryWrapper.build();
            String sql = "DELETE FROM " + tableName + condition;
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            log.info("执行SQL: " + preparedStatement);
            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log(e.getMessage());
            return 0;
        }
    }


    /**
     * 插入新的数据库记录
     *
     * @param obj 包含新数据的对象
     * @param <T> 对象的类型
     * @return 受影响的记录数
     */
    public static <T> int insert(T obj) {
        try (Connection conn = getConnection()) {
            Class<?> clazz = obj.getClass();
            String tableName = getTableName(clazz);
            String primaryKey = getPrimaryKeyFieldName(clazz);
            StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ").append(tableName).append(" (");

            Field[] fields = clazz.getDeclaredFields();
            int parameterIndex = 1; // 参数索引从1开始
            int excludeCount = 0;
            for (Field field : fields) {
                if (field.isAnnotationPresent(Exclude.class)) {
                    excludeCount++;
                    continue;
                }
                if (parameterIndex > 1) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append(getColumnName(field));
                parameterIndex++;
            }

            if (primaryKey.isEmpty()) {
                throw new RuntimeException("你的实体类" + clazz.getName() + "没有指定主键");
            }
            sqlBuilder.append(") VALUES (");

            for (int i = 0; i < fields.length - excludeCount; i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append("?");
            }
            sqlBuilder.append(")");

            String sql = sqlBuilder.toString();

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Field field : fields) {
                    if (field.isAnnotationPresent(Exclude.class)) {
                        continue;
                    }
                    field.setAccessible(true);
                    pstmt.setObject(parameterIndex, field.get(obj));
                    parameterIndex++;
                }
                if (logging) {
                    log.log(Level.INFO, "执行SQL: " + pstmt);
                }
                return pstmt.executeUpdate();
            }
        } catch (SQLException | IllegalAccessException e) {
            log(e.getMessage());
            return 0;
        }
    }


    /**
     * 保存数据库记录,若主键重复更新,不存在插入
     *
     * @param obj 包含新数据的对象
     * @param <T> 对象的类型
     */
    public static <T> void save(T obj) {
        try {
            Connection conn = getConnection();
            Class<?> clazz = obj.getClass();
            String tableName = getTableName(clazz);

            StringBuilder sqlBuilder = new StringBuilder("REPLACE INTO ").append(tableName).append(" (");
            StringBuilder placeholders = new StringBuilder();
            List<Object> values = new ArrayList<>();

            Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if (field.isAnnotationPresent(Exclude.class)) {
                    continue;
                }
                field.setAccessible(true);
                if (i > 0) {
                    sqlBuilder.append(", ");
                    placeholders.append(", ");
                }
                String columnName = getColumnName(field);
                sqlBuilder.append(columnName);
                placeholders.append("?");
                values.add(field.get(obj));
            }
            sqlBuilder.append(") VALUES (").append(placeholders).append(")");

            String sql = sqlBuilder.toString();
            log("执行SQL: " + sql);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < values.size(); i++) {
                    pstmt.setObject(i + 1, values.get(i));
                }
                pstmt.executeUpdate();
            }
        } catch (SQLException | IllegalAccessException e) {
            log(e.getMessage());
        }
    }


    // 获取表名
    private static String getTableName(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Table.class)) {
            Table table = clazz.getAnnotation(Table.class);
            return table.value();
        } else {
            // 默认使用类名作为表名
            return clazz.getSimpleName();
        }
    }

    // 获取主键字段名
    private static String getPrimaryKeyFieldName(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                return field.getName();
            }
        }
        return ""; // 没有找到主键字段
    }

    // 获取主键值
    private static Object getPrimaryKeyValue(Object obj) throws IllegalAccessException {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                return field.get(obj);
            }
        }
        throw new RuntimeException("实体类" + clazz.getName() + "没有指定主键");
    }

    // 获取列名
    private static String getColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            if (!column.value().isEmpty()) {
                return column.value();
            }
        }
        // 默认使用字段名，将驼峰命名转换为下划线分隔的形式
        return camelToSnakeCase(field.getName());
    }

    // 驼峰命名转换为下划线分隔的形式
    private static String camelToSnakeCase(String str) {
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    // 将结果集映射为实体对象
    private static <T> T mapResultSetToEntity(ResultSet rs, Class<T> clazz) {
        try {
            T obj = clazz.getDeclaredConstructor().newInstance();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Exclude.class)) {
                    continue;
                }
                field.setAccessible(true);
                String fieldName = getColumnName(field);
                Object value = rs.getObject(fieldName);
                if (value != null && !(value instanceof List)) {
                    setFieldValue(obj, field, value);
                }
            }
            return obj;
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            log(e.getMessage());
            return null;
        }

    }

    // 设置实体对象的字段值
    private static void setFieldValue(Object obj, Field field, Object value) throws IllegalAccessException {
        Type type = field.getType();
        if (type == String.class) {
            field.set(obj, value.toString());
        } else if (type == Integer.class || type == int.class) {
            field.set(obj, Integer.valueOf(value.toString()));
        } else if (type == Long.class || type == long.class) {
            field.set(obj, Long.valueOf(value.toString()));
        } else if (type == Double.class || type == double.class) {
            field.set(obj, Double.valueOf(value.toString()));
        } else if (type == Boolean.class || type == boolean.class) {
            field.set(obj, Boolean.valueOf(value.toString()));
        } else if (type == Date.class) {
            field.set(obj, toDate(value));
        } else {
            log.log(Level.WARNING, field.getName() + " 不支持类型: " + type.getTypeName());
        }
    }

}
