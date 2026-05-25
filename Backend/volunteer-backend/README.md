<<<<<<< HEAD
# 数计学院志愿者服务小程序

本项目包含两个目录：

- `volunteer-miniprogram`：微信小程序原生前端，WXML + WXSS + JS + JSON。
- `volunteer-backend`：Spring Boot 后端，RESTful API + MySQL。

## 一、创建 MySQL 数据库

进入 MySQL 后依次执行：

```sql
source D:/IDEA Projects/volunteer-backend/sql/schema.sql;
source D:/IDEA Projects/volunteer-backend/sql/data.sql;
```

默认库名：`volunteer_service`。

如你的 MySQL 账号密码不是 `root/root`，修改：

```text
volunteer-backend/src/main/resources/application.yml
```

## 二、启动 Spring Boot 后端

需要 JDK 17 和 Maven。

```bash
cd volunteer-backend
mvn spring-boot:run
```

默认服务地址：

```text
http://localhost:8080
```

## 三、导入微信开发者工具

1. 打开微信开发者工具。
2. 选择“导入项目”。
3. 项目目录选择 `volunteer-miniprogram`。
4. AppID 可使用测试号或导入时选择游客模式。
5. 本地调试时关闭“校验合法域名”。

## 四、修改接口 baseUrl

小程序接口地址在：

```text
volunteer-miniprogram/app.js
```

默认：

```js
baseUrl: 'http://localhost:8080'
```

如果用真机预览，需要改成电脑局域网 IP，例如：

```js
baseUrl: 'http://192.168.1.10:8080'
```

## 五、测试账号

管理员：

- 账号：`admin`
- 密码：`123456`

志愿者：

- 账号：`20240001`
- 密码：`123456`
- 账号：`20240002`
- 密码：`123456`

## 六、主要接口

所有非登录注册接口都需要请求头：

```text
Authorization: volunteer-token-{userId}-{role}
```

接口返回统一格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 方法 | 地址 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 登录 | 公开 |
| POST | `/api/auth/register` | 志愿者注册 | 公开 |
| GET | `/api/activities` | 活动列表，支持 `category/status/keyword` | 登录 |
| GET | `/api/activities/{id}` | 活动详情 | 登录 |
| POST | `/api/activities` | 发布活动 | ADMIN |
| PUT | `/api/activities/{id}` | 编辑活动 | ADMIN |
| DELETE | `/api/activities/{id}` | 删除活动 | ADMIN |
| POST | `/api/registrations` | 志愿者报名 | VOLUNTEER |
| GET | `/api/registrations/my` | 我的报名 | 登录 |
| PUT | `/api/registrations/{id}/review` | 审核报名 | ADMIN |
| GET | `/api/volunteers` | 志愿者人才库，支持 `keyword/major/skill` | 登录 |
| GET | `/api/volunteers/{id}` | 志愿者详情 | 登录 |
| GET | `/api/match/activity/{activityId}` | 活动推荐志愿者 Top 5 | ADMIN |
| GET | `/api/admin/statistics` | 后台统计 | ADMIN |

## 七、数据库表

- `user`：账号、姓名、学号/工号、手机号、角色。
- `volunteer_profile`：学院、专业班级、技能标签、可服务时间、累计时长、信用评分、服务次数。
- `activity`：活动名称、类别、地点、起止时间、招募人数、已报名人数、技能要求、描述、状态。
- `registration`：活动报名、审核状态、审核意见。
- `service_record`：服务记录、服务时长、完成说明。

## 八、项目亮点

- 学生端与管理员端角色分流。
- 活动报名支持重复报名拦截、满员拦截、状态控制。
- 管理员可审核报名，完成后自动累加志愿者服务时长和服务次数。
- 志愿者人才库支持姓名、专业、技能标签检索。
- 智能匹配按技能 60%、时间 20%、信用评分 20% 计算推荐分。
- 后台提供活动总数、志愿者总数、报名总数、累计服务时长、类别统计和最近报名记录。
- 前端所有页面均通过 `utils/request.js` 对接后端接口，没有纯 mock 数据。
=======
# MiniProgram_Volunteer
参加院内举行的关于志愿者的小程序设计大赛而做的项目
>>>>>>> 5d3183f6b1310d14a9177b5d1145b34da1a408db
