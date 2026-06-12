# 大学生英语学习平台后端

## 技术栈

- Spring Boot
- Spring Web
- Spring Data JPA
- MySQL
- 本地 Ollama

## 默认配置

配置文件在 `src/main/resources/application.yml`。

- 数据库名：`english_study_platform`
- 默认数据库地址：`jdbc:mysql://127.0.0.1:3306/english_study_platform`
- 默认数据库账号：`root`
- 默认数据库密码：`123456`
- 默认前端地址：`http://localhost:5173`
- 默认 Ollama 地址：`http://127.0.0.1:11434`
- 默认模型：`qwen2.5:7b`

## 初始化数据库

先执行：

`sql/english_study_platform.sql`

SQL 中包含建库、建表和演示数据。

## 演示账号

- 学生：`student1 / 123456`
- 教师：`teacher1 / 123456`
- 管理员：`admin / 123456`

## 说明

- 学生注册默认创建学生角色账号。
- AI 词典不落库，直接请求本地 Ollama。
- 当前实现是偏毕设演示版，代码尽量保持简单，重点是功能能跑通。
