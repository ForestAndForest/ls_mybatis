# Ls-mybati

## 简介

`JDBCUtils` 是一个基于 Java 的 JDBC 封装工具类，用于简化 JDBC 操作，提供了常用的数据库操作方法。

## 功能特点

- 自动注册 MySQL 驱动程序
- 提供获取数据库连接的方法
- 执行 SQL 查询并返回结果集
- 查询所有记录
- 返回符合条件的记录
- 查询符合条件的第一条记录
- 保存对象到数据库表
- 删除符合条件的记录
- 将驼峰命名转换为下划线命名

## 使用方法

1. 引入工具类到你的 Java 项目中。
2. 配置 `application.properties` 文件，设置数据库连接信息。
3. 创建Java实体和数据库表
4. 使用工具类提供的方法进行数据库操作。


## 使用示例

### 数据库表结构

#### 用户表（user）

| 字段名     | 数据类型 | 说明          |
| ---------- | -------- | ------------- |
| id         | int      | 主键          |
| name       | varchar  | 用户名        |
| age        | int      | 年龄          |
| createdTime| Long | 创建时间戳      |

#### user表查询

| id | name   | age | createdTime          |
|----|--------|-----|----------------------|
| 1  | Alice  | 25  | 1700000000000 |
| 2  | Bob    | 30  | 1700000000000 |


### Java User实体

```java
@Table(value = "user")
/**
 * 注解@Table的作用是将Java对象映射到数据库中的表上。
 * 若不写值，默认为类名。例如，@Table("user") 将该类映射到数据库中名为 "user" 的表。
 */
public class User {
    @Id
    /**
     * 注解@Id的作用是将Java对象的属性映射到数据库中的主键上。
     * 若不写值，默认为 "id" 属性。例如，@Id("id") 将该属性映射为数据库中的主键。
     */
    private int id;

    @Column
    /**
     * 注解@Column的作用是将Java对象的属性映射到数据库中的列上。
     * 若不写值，默认为字段名。例如，@Column("name") 将该属性映射为数据库中名为 "name" 的列。
     * 若Java字段名满足驼峰命名规则，数据库字段名满足下划线命名规则，则不需要写@Column注解。
     * 例如，Java字段名为 "userName"，数据库字段名为 "user_name"。
     */
    private String name;

    private int age;
    private String createdTime;

    //省略getter/setter
}
```
### JDBCUtils使用

```java
// 查询所有记录
List<User> userList = JDBCUtils.select(User.class);

// 查询符合条件的记录
QueryWrapper<User> queryWrapper = new QueryWrapper<>();
queryWrapper.eq("name", "Alice");
List<User> userList = JDBCUtils.select(User.class, queryWrapper);

// 查询符合条件的第一条记录
User user = JDBCUtils.selectOne(User.class, queryWrapper);

// 保存对象到数据库表
User newUser = new User();
newUser.setName("Bob");
newUser.setAge(25);
JDBCUtils.save(newUser);

// 删除符合条件的记录
QueryWrapper<User> deleteWrapper = new QueryWrapper<>();
deleteWrapper.eq("id", 1);
JDBCUtils.delete(User.class, deleteWrapper);
```

## 注意事项

- 在使用工具类之前，确保已正确配置数据库连接信息。
- 请根据实际情况调整工具类中的方法，以满足项目需求。
