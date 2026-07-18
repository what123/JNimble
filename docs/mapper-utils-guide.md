# MapperUtils 使用说明

## 1. 定位

`MapperUtils` 是 JNimble 平台层统一的数据访问工具。

平台数据库模式使用：

```text
MyBatis-Plus BaseMapper + Entity + MapperUtils
```

规则：

- 单表新增、删除、修改、查询必须走 `MapperUtils`。
- Service 层不能直接调用 `BaseMapper.insert/update/delete/select*`。
- `BaseMapper` 只作为 `MapperUtils` 的底层执行器。
- join 查询、批量同步、跨表权限校验可以写 MyBatis Mapper 自定义方法。
- 不引入 Lombok。
- 不使用固定 `Long id`。

## 2. 包结构

```text
jnimble-platform/src/main/java/com/jnimble/platform/persistence/
├── crud/
│   ├── BaseSearchCondition.java
│   ├── MapperUtils.java
│   ├── PageSearchRequest.java
│   ├── SPage.java
│   ├── SearchFilter.java
│   ├── SearchFilterValue.java
│   └── WrapperType.java
├── entity/
│   ├── AuditLogEntity.java
│   ├── PermissionEntity.java
│   ├── PluginStateEntity.java
│   ├── RoleEntity.java
│   ├── RolePermissionEntity.java
│   ├── SubjectRoleEntity.java
│   └── UserEntity.java
└── mapper/
    ├── AuditLogMapper.java
    ├── PermissionMapper.java
    ├── PluginStateMapper.java
    ├── RoleMapper.java
    ├── RolePermissionMapper.java
    ├── SubjectRoleMapper.java
    └── UserMapper.java
```

## 3. 配置

JNimble 使用数据库模式，需要提供 `DataSource`，并执行系统 Flyway 迁移。

## 4. 方法清单

```java
MapperUtils.insert(mapper, entity);
MapperUtils.updateById(mapper, entity);
MapperUtils.updateOne(mapper, entity, EntityClass.class, wrapper -> ...);
MapperUtils.updateByCondition(mapper, entity, EntityClass.class, wrapper -> ...);
MapperUtils.getById(mapper, id, missingMessage);
MapperUtils.selectOne(mapper, EntityClass.class, wrapper -> ...);
MapperUtils.deleteById(mapper, id);
MapperUtils.deleteByCondition(mapper, EntityClass.class, wrapper -> ...);
MapperUtils.existsByCondition(mapper, EntityClass.class, wrapper -> ...);
MapperUtils.selectList(mapper, EntityClass.class, wrapper -> ...);
MapperUtils.selectByOffset(mapper, EntityClass.class, request, wrapper -> ...);
MapperUtils.buildWrapper(EntityClass.class, condition, defaultSort);
MapperUtils.orderBy(wrapper, EntityClass.class, sort, defaultSort);
```

## 5. 使用规则

Service 层必须这样写：

```java
MapperUtils.insert(userMapper, entity);

MapperUtils.updateOne(userMapper, update, UserEntity.class,
        wrapper -> wrapper.eq("username", username));

MapperUtils.selectOne(userMapper, UserEntity.class,
        wrapper -> wrapper.eq("username", username));

MapperUtils.deleteByCondition(subjectRoleMapper, SubjectRoleEntity.class,
        wrapper -> wrapper.eq("subject_id", subjectId).eq("role_id", roleId));
```

不要这样写：

```java
userMapper.insert(entity);
userMapper.updateById(entity);
userMapper.selectById(id);
userMapper.selectList(wrapper);
userMapper.delete(wrapper);
```

例外：

- `MapperUtils` 内部可以直接调用 `BaseMapper`。
- Mapper 接口可以声明 join、批量同步、跨表权限校验等自定义方法。

## 6. 字段规则

Entity 使用 camelCase 字段：

```java
private String pluginId;
private Instant createdAt;
```

数据库字段使用 snake_case：

```text
plugin_id
created_at
```

`MapperUtils` 根据 Entity 字段生成白名单，支持前端和调用方传入 camelCase 或 snake_case。

未知字段会抛出异常：

```text
Unknown field: xxx
```

## 7. 查询过滤

`BaseSearchCondition` 支持动态 filters：

```java
SearchFilter filter = new SearchFilter();
filter.setKey("username");
filter.setMatch("eq");
filter.setKeyword("admin");
```

支持的 match：

```text
eq
like
in
gt
ge
lt
le
between
zero_to_null_eq
```

## 8. 排序

排序格式：

```text
created_at desc
plugin_id asc, code asc
```

规则：

- 排序字段必须命中 Entity 字段白名单。
- 排序方向只允许 `asc`、`desc`。
- 不允许拼接未校验字段。

## 9. 分页

分页请求：

```java
PageSearchRequest<UserSearch> request = new PageSearchRequest<>();
request.getPage().setOffset(0);
request.getPage().setSize(20);
request.getPage().setSort("username asc");
```

分页查询：

```java
SPage<UserEntity> page = MapperUtils.selectByOffset(
        userMapper,
        UserEntity.class,
        request,
        wrapper -> wrapper.eq("status", "ACTIVE")
);
```

分页实现当前使用：

```text
selectCount + limit/offset
```

`size` 最大限制为 `500`。

## 10. 复合主键表

复合主键表：

```text
jnimble_role_permission(role_id, permission_code)
jnimble_subject_role(subject_id, role_id)
```

规则：

- 不使用 `selectById`。
- 单表操作使用 `MapperUtils` 条件方法。

示例：

```java
MapperUtils.existsByCondition(rolePermissionMapper, RolePermissionEntity.class,
        wrapper -> wrapper.eq("role_id", roleId)
                .eq("permission_code", permissionCode));
```

## 11. 自定义 Mapper 方法

以下情况允许写 Mapper 自定义方法：

- join 查询
- 批量同步
- 跨表权限校验
- 数据库方言相关 SQL

示例：

```java
@Select("""
        select count(*)
          from jnimble_subject_role sr
          join jnimble_role r on r.id = sr.role_id
          join jnimble_role_permission rp on rp.role_id = r.id
          join jnimble_permission p on p.code = rp.permission_code
         where sr.subject_id = #{subjectId}
           and rp.permission_code = #{permissionCode}
           and r.status = #{roleStatus}
           and rp.status = #{permissionStatus}
           and p.status = #{permissionStatus}
        """)
int countSubjectPermission(...);
```

## 12. 当前平台实现

```text
MybatisUserAccountService
MybatisAuditService
MybatisPluginStateStore
MybatisAccessControlService
```

## 13. 验证命令

```bash
source .tools/env.sh
mvn -s .tools/maven/settings-cn.xml \
  -Dmaven.repo.local=/home/tanxy/workspace/JNimble/.tools/m2 \
  test
```

旧实现扫描：

```bash
rg -n "JdbcTemplate|RowMapper|org\.springframework\.jdbc|jdbc-enabled" \
  jnimble-platform jnimble-starter README.md docs/framework-design.md docs/plugin-development-guide.md
```

该命令应无输出。
