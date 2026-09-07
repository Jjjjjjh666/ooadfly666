# 售后与服务系统

本仓库整合了售后/服务系统的后端代码与面向对象分析设计资料，方便在同一个位置查看需求、模型、接口设计和实现。

## 项目简介

系统围绕电商场景中的售后单、服务单和服务商管理展开，主要包含：

- 售后业务：退货、换货、维修等售后单的受理、确认、收货、处理与取消。
- 服务业务：服务商开户与变更审核、服务单创建、派工、完成与取消。
- 公共能力：统一返回结果、分页、鉴权及模块间共享代码。
- 分析设计：需求规格、用例、界面原型、领域模型、状态图、数据库模型、API、类图和顺序图。

## 技术栈

- Java 17
- Spring Boot 3.2.5
- Spring Cloud 2023.0.1 / OpenFeign
- Maven 多模块
- MyBatis、MySQL、Druid
- Lombok
- JUnit、Mockito、JaCoCo

## 仓库结构

```text
.
├── core/                 # 公共基础模块
├── aftersale/            # 售后业务模块，默认端口 8081
├── service/              # 服务与服务商模块，默认端口 8082
├── sql/                  # 数据库初始化脚本
├── docs/
│   └── design/           # 需求、原型、UML、API 及课程设计资料
└── pom.xml               # Maven 聚合工程
```

设计资料来自原 [ooadfly666](https://github.com/Jjjjjjh666/ooadfly666) 仓库，代码来自原 [aftersale-service-system](https://github.com/Jjjjjjh666/aftersale-service-system) 仓库。

## 快速开始

### 1. 环境要求

- JDK 17
- Maven 3.8+
- MySQL 8.x

### 2. 初始化数据库

分别执行以下脚本：

- `sql/aftersale_db(1).sql`
- `sql/service_db(1).sql`

### 3. 配置数据库连接

两个业务模块均支持通过环境变量覆盖数据库配置：

```bash
export MYSQL_HOST=127.0.0.1
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password
```

数据库名默认分别为 `aftersale_db` 和 `service_db`，也可在启动单个模块时通过 `MYSQL_DATABASE` 覆盖。

> 配置文件中的默认连接参数仅供开发使用。部署或共享环境中请务必通过环境变量设置自己的数据库地址和密码。

### 4. 构建与测试

```bash
mvn clean test
```

### 5. 启动服务

先启动服务模块：

```bash
mvn -pl service -am spring-boot:run
```

再启动售后模块：

```bash
mvn -pl aftersale -am spring-boot:run
```

默认访问地址：

- 服务模块：`http://localhost:8082`
- 售后模块：`http://localhost:8081`

售后模块通过 `SERVICE_BASE_URL` 调用服务模块，默认值为 `http://127.0.0.1:8082`。

## 设计资料

[`docs/design/`](docs/design/) 按原资料结构保存，包含：

- 需求规格说明书
- 用例与界面原型
- 领域模型、状态图、数据库设计与 API
- 售后详细设计：类图与顺序图
- 服务详细设计：类图与顺序图
- 汇总版课程设计资料

其中 `.mdj` 文件可使用 StarUML 打开；HTML 原型和 API 文档可直接用浏览器查看。

## 说明

当前仓库主要用于课程设计、学习和演示。部分接口依赖物流等外部服务，独立运行前请根据实际环境调整服务地址与相关配置。
