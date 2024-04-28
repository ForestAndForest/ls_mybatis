# Ls-mybati

## 简介

`Ls-mybati` 是一个基于 Java 的 JDBC 封装库，用于简化 JDBC 操作，提供了常用的数据库操作方法。

## 功能特点

- 自动注册 MySQL 驱动程序
- 提供获取数据库连接的方法
- 执行 SQL 查询并返回结果集
- 查询所有记录
- 返回符合条件的记录
- 查询符合条件的第一条记录
- 保存对象到数据库表
- 删除符合条件的记录
- 分组统计数量

## 使用方法

1. 引入工具类到你的 Java 项目中。
2. 配置 `application.properties` 文件，设置数据库连接信息。
3. 创建Java实体和数据库表
4. 使用工具类提供的方法进行数据库操作。


## 使用示例

### 创建application.properties
```properties
#mysql
db.url=jdbc:mysql://localhost:3306/database
db.username=username
db.password=password
db.driver=com.mysql.cj.jdbc.Driver
db.logging=true
```
你可能需要额外引入mysql驱动

### 数据库表结构

#### 用户表（user）

| 字段名     | 数据类型 | 说明          |
| ---------- | -------- | ------------- |
| id         | int      | 主键          |
| name       | varchar  | 用户名        |
| age        | int      | 年龄          |
| created_time| Long | 创建时间戳      |

#### user表查询

| id | name   | age | created_time          |
|----|--------|-----|----------------------|
| 1  | Alice  | 25  | 1700000000000 |
| 2  | Bob    | 30  | 1700000000000 |


### Java User实体

```java
@Table(value = "user")
/**
 * 若不写该注解则默认使用类名为表名(user)
 * 注解@Table的作用是将Java对象映射到数据库中的表上。
 * 若不写值，默认为类名。例如，@Table("user") 将该类映射到数据库中名为 "user" 的表。
 */
public class User {
    @Id
    /**
     * 必须指定主键
     * 注解@Id的作用是将Java对象的属性映射到数据库中的主键上。
     * 若不写值，默认为 "id" 属性。例如，@Id("id") 将该属性映射为数据库中的主键。
     */
    private int id;

    @Column
    /**
     * 若不写该注解则默认使用属性名作为列名(name)
     * 注解@Column的作用是将Java对象的属性映射到数据库中的列上。
     * 若不写值，默认为字段名。例如，@Column("name") 将该属性映射为数据库中名为 "name" 的列。
     * 若Java字段名满足驼峰命名规则，数据库字段名满足下划线命名规则，则不需要写@Column注解。
     * 例如，Java字段名为 "userName"，数据库字段名为 "user_name"。
     */
    private String name;

    private int age;

    // @Column("create_time") //这里指定time字段映射到create_time列
    private Long createdTime;

    @Exclude
    /**
     * 注解@Exclude的作用是忽略被注解的字段
     * 在进行数据库操作时将被忽略
     */
    private String password;

    //省略getter/setter
}
```
### JDBCUtils使用

#### 查询所有记录

```java
// 查询user表所有记录
List<User> users;
//1.使用JDBCUtils.select(Class<T> clazz)方法查询
users =  JDBCUtils.select(User.class);

//2.使用JDBCUtils.select(Class<T> clazz, QueryWrapper<T> queryWrapper)方法查询
QueryWrapper<User> queryWrapper = new QueryWrapper<>();
users = JDBCUtils.select(User.class, queryWrapper); //queryWrapper未添加任何条件

System.out.println(JSON.toJSON(users)); //JSON.toJSON() 将对象转换为JSON字符串
```
打印结果
```json
[
    {
        "age":25,
        "createdTime":"1700000000000",
        "id":1,
        "name":"Alice"
    },
    {
        "age":30,
        "createdTime":"1700000000000",
        "id":2,
        "name":"Bob"
    }
]
```

#### 条件查询

```java
//使用JDBCUtils.select(Class<T> clazz, QueryWrapper<T> queryWrapper)方法查询
//queryWrapper.eq(SFunction<T, ?> fn, Object value)
// 第一个参数 方法引用https://www.runoob.com/java/java8-method-references.html
// 第二个参数 条件表达式右侧的值
// 支持链式调用 也可以单独使用
QueryWrapper<User> queryWrapper = new QueryWrapper<>();
queryWrapper
  .eq(User::getName, "Alice")  //eq 表示等于 <=> name = 'Alice'
  .gt(User::getAge, 18)  //gt 表示大于 <=> age > 18
  .lt(User::getAge, 30)  //lt 表示小于 <=> age < 30
  .like(User::getName, "Alice");  //like 表示模糊查询 <=> name like '%Alice%'

List<User> users = JDBCUtils.select(User.class, queryWrapper);
System.out.println(JSON.toJSON(users)); //JSON.toJSON() 将对象转换为JSON字符串
```
生成的sql语句

`SELECT * FROM user_tb WHERE name = 'Alice' AND age > 18 AND age < 30 AND name LIKE '%Alice%'`

打印结果
```json
[
    {
        "age":25,
        "createdTime":"1700000000000",
        "id":1,
        "name":"Alice"
    }
]
```

#### 查询满足条件的第一条记录
```java
//使用JDBCUtils.select(Class<T> clazz, QueryWrapper<T> queryWrapper)方法查询
//queryWrapper.eq(SFunction<T, ?> fn, Object value)
// 第一个参数 方法引用https://www.runoob.com/java/java8-method-references.html
// 第二个参数 条件表达式右侧的值
// 支持链式调用 也可以单独使用
QueryWrapper<User> queryWrapper = new QueryWrapper<>();
queryWrapper.gt(User::getAge, 18);  //gt 表示大于 <=> age > 18

User users = JDBCUtils.selectOne(User.class, queryWrapper);
System.out.println(JSON.toJSON(users)); //JSON.toJSON() 将对象转换为JSON字符串
```
打印结果
```json
[
    {
        "age":25,
        "createdTime":"1700000000000",
        "id":1,
        "name":"Alice"
    }
]
```

#### 保存对象和修改对象
```java
User user = new User();
//user.setId(2) //未设置主键 插入数据
user.setAge(99);
user.setName("老王");
user.setCreatedTime(System.currentTimeMillis());
        
int i = JDBCUtils.save(user);//若存在相同主键，则更新，否则插入 返回插入记录数量
System.out.println(i);
```
打印结果

`1`

数据表(user)
| id | name   | age | created_time          |
|----|--------|-----|----------------------|
| 1  | Alice  | 25  | 1700000000000 |
| 2  | Bob    | 30  | 1700000000000 |
| 3 | 老王 | 99 | 1714230676389 |

#### 删除记录
```java
QueryWrapper<User> queryWrapper = new QueryWrapper<>();
queryWrapper.eq(User::getName,"老王");//删除name="老王"的记录
int i = JDBCUtils.delete(User.class, queryWrapper);
System.out.println(i);
```
打印结果

`1`

数据表(user)
| id | name   | age | created_time          |
|----|--------|-----|----------------------|
| 1  | Alice  | 25  | 1700000000000 |
| 2  | Bob    | 30  | 1700000000000 |


## 注意事项

- 在使用工具类之前，确保已正确配置数据库连接信息。
- 实体类需要提供默认构造方法和相应的 getter 和 setter 方法。
- 实体类的字段需要与数据库表的字段对应。
- 主键字段需要使用 @Id 注解标注。
- 请根据实际情况调整工具类中的方法，以满足项目需求。

## 联系方式

如有任何问题或建议，请通过以下方式联系我：
- Email:2900221581@qq.com
- QQ:2900221581
- 微信:CF154805214
