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
3. 使用工具类提供的方法进行数据库操作。

## 使用示例

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
