package ls_mybatis.utils;


import ls_mybatis.annotation.Column;
import ls_mybatis.annotation.Table;
import ls_mybatis.core.QueryWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author 29002
 * @version v0.1
 */
@SuppressWarnings("all")
public class JDBCUtils {
    private static final Logger log = Logger.getLogger(JDBCUtils.class.getName());
    private static Connection connection;

    static {
        System.out.println("---------------ls-mybatis v0.1---------------");
        System.out.println("---------------author:29002---------------");
        connection = setupConnection();
    }

    private static Connection setupConnection() {
        try {
            Properties properties = new Properties();
            InputStream inputStream = JDBCUtils.class.getClassLoader().getResourceAsStream("application.properties");
            properties.load(inputStream);
            String url = properties.getProperty("db.url");
            String username = properties.getProperty("db.username");
            String password = properties.getProperty("db.password");
            //自动注册mysql驱动
            Class.forName(properties.getProperty("db.driver"));
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e + "\n修复建议:检查你的db.url db.username db.password是否存在及值是否正确");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e + "\n修复建议:添加mysql驱动");
        } catch (IOException e) {
            throw new RuntimeException("application.properties文件不存在!\n请创建该文件并写入\ndb.url=你的数据库url\ndb.username=用户名\ndb.password=密码\ndb.driver=数据库驱动全限定类名");
        }
    }


    // 获取数据库连接方法
    public static Connection getConnection() {
        return connection;
    }


    // 查询方法，执行 SQL 查询并返回结果集
    public static ResultSet executeQuery(String sql) {
        try {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            return rs;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 查询所有记录
     * @param clazz 实体类
     * @return
     * @param <T>
     */
    public static <T> List<T> select(Class<T> clazz) {
        return select(clazz, null);
    }

    /**
     * 返回所有符合条件的记录
     *
     * @param queryWrapper
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> select(Class<T> clazz, QueryWrapper<T> queryWrapper) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        //通过注解反射获得表名
        String tableName = "";
        if (clazz.isAnnotationPresent(Table.class)) {
            Table table = clazz.getAnnotation(Table.class);
            tableName = table.value();
        } else {
            throw new RuntimeException("该类不是实体\n修复建议:添加@Table('表名')注解");
        }

        try {
            conn = getConnection();
            stmt = conn.createStatement();

            // 构建 SQL 查询语句
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ").append(tableName);

            if (Objects.nonNull(queryWrapper)) {
                sqlBuilder.append(" WHERE ");
                sqlBuilder.append(queryWrapper.build());
            }

            String sql = sqlBuilder.toString();
            log.info("SQL: " + sql);
            rs = stmt.executeQuery(sql);

            List<T> result = new ArrayList<>();

            // 遍历结果集, 将结果集转换为对象列表 反射机制
            while (rs.next()) {
                T obj = clazz.getDeclaredConstructor().newInstance();
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    Type type = field.getType();
                    Object value = rs.getObject(camelToSnakeCase(fieldName));
                    if (value != null && type != List.class) {
                        switch (type.getTypeName()) {
                            case "java.lang.String":
                                field.set(obj, value.toString());
                                break;
                            case "java.lang.Integer":
                                field.set(obj, Integer.parseInt(value.toString()));
                                break;
                            case "java.lang.Long":
                                field.set(obj, Long.parseLong(value.toString()));
                                break;
                            case "java.lang.Double":
                                field.set(obj, Double.parseDouble(value.toString()));
                                break;
                            case "int":
                                field.set(obj, Integer.parseInt(value.toString()));
                                break;
                            case "boolean":
                                field.set(obj, Boolean.parseBoolean(value.toString()));
                                break;
                            default:
                                log.info(field.getName() + "  Unsupported type: " + type.getTypeName());
                        }
                    }
                }
                result.add(obj);
            }
            return result;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 返回符合条件的第一条记录
     *
     * @param queryWrapper
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T selectOne(Class<T> clazz, QueryWrapper<T> queryWrapper) {
        //TODO 待优化,仅查询第一条记录
        List<T> users = select(clazz, queryWrapper);
        if (users.size() == 0) {
            return null;
        }
        return users.get(0);
    }

    public static <T> int save(T obj, String primaryKey) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            Class<?> clazz = obj.getClass();
            Field field = clazz.getDeclaredField(primaryKey);
            field.setAccessible(true);
            Object primaryKeyValue = field.get(obj);

            conn = getConnection();
            //通过注解反射获得表名
            String tableName = "";
            if (clazz.isAnnotationPresent(Table.class)) {
                Table table = clazz.getAnnotation(Table.class);
                tableName = table.value();
            } else {
                throw new RuntimeException("该类不是实体");
            }

            StringBuilder sqlBuilder = new StringBuilder("REPLACE INTO ").append(tableName).append(" (");

            Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append(camelToSnakeCase(fields[i].getName()));
            }
            sqlBuilder.append(") VALUES (");

            for (int i = 0; i < fields.length; i++) {
                if (i > 0) {
                    sqlBuilder.append(", ?");
                } else {
                    sqlBuilder.append("?");
                }
            }
            sqlBuilder.append(")");

            String sql = sqlBuilder.toString();
            log.info(sql);
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < fields.length; i++) {
                fields[i].setAccessible(true);
                pstmt.setObject(i + 1, fields[i].get(obj));
            }
            return pstmt.executeUpdate();

        } catch (SQLException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static <T> int save(T obj) {
        return save(obj, "id"); // 默认主键名为 "id"
    }

    public static <T> int delete(Class<?> clazz, QueryWrapper<T> queryWrapper) {
        Connection conn = null;
        Statement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.createStatement();

            //通过注解反射获得表名
            String tableName = "";
            if (clazz.isAnnotationPresent(Table.class)) {
                Table table = clazz.getAnnotation(Table.class);
                tableName = table.value();
            } else {
                throw new RuntimeException("该类不是实体");
            }

            String condition = queryWrapper.build();

            String sql = "DELETE FROM " + tableName + " WHERE " + condition;
            log.info("SQL: " + sql);

            return stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static <T> int insert(T obj) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            Class<?> clazz = obj.getClass();
            conn = getConnection();
            String tableName = clazz.getSimpleName();
            StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ").append(tableName).append(" (");

            Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append(camelToSnakeCase(fields[i].getName()));
            }
            sqlBuilder.append(") VALUES (");

            for (int i = 0; i < fields.length; i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append("?");
            }
            sqlBuilder.append(")");

            String sql = sqlBuilder.toString();
            log.info(sql);
            pstmt = conn.prepareStatement(sql);

            for (int i = 0; i < fields.length; i++) {
                fields[i].setAccessible(true);
                pstmt.setObject(i + 1, fields[i].get(obj));
            }

            return pstmt.executeUpdate();

        } catch (SQLException | IllegalAccessException e) {
            e.printStackTrace();
            return 0;
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
