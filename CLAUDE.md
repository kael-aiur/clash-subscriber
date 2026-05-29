# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Clash 订阅管理中心 — 一个管理多个 Clash/Mihomo 订阅源的服务。核心功能：
- 保存和管理多个服务商的 Clash 订阅链接
- 从订阅源获取完整 Clash 配置（通过 User-Agent 请求头触发服务商返回完整配置而非纯 base64 节点列表）
- 对多订阅源的节点进行组合、修改出站规则等前置处理
- 将处理后的配置写入后端 Mihomo 实例
- 支持多 Mihomo 节点的动态管理和选择
- 支持从订阅源到 Mihomo 的定时刷新

## Tech Stack

- **Language**: Java 21
- **Build**: Maven (group: `site.kael.clash`, artifact: `clash-subscriber`)
- **Encoding**: UTF-8

## Build Commands

```bash
# 编译
mvn compile

# 运行测试
mvn test

# 运行单个测试
mvn test -Dtest=ClassName#methodName

# 打包
mvn package

# 跳过测试打包
mvn package -DskipTests
```

## Architecture

项目采用分层架构：

```
┌─────────────────────────────────┐
│  REST API 层                     │  订阅管理、Mihomo 节点管理、配置推送
├─────────────────────────────────┤
│  订阅处理层                      │  订阅获取、解析、节点组合、规则处理
├─────────────────────────────────┤
│  Mihomo 对接层                   │  配置写入、实例管理、健康检查
├─────────────────────────────────┤
│  持久层                          │  订阅源存储、Mihomo 节点存储、配置历史
└─────────────────────────────────┘
```

核心模块：
- **订阅源管理**: CRUD 操作，存储服务商订阅链接及请求参数
- **订阅获取与解析**: HTTP 请求（带 User-Agent 头），解析 Clash YAML 配置，处理纯 base64 回退
- **配置前置处理**: 多订阅节点合并、出站规则修改、规则集过滤等 Pipeline 处理
- **Mihomo 实例管理**: 多节点注册、选择、健康检查，配置推送 API 对接
- **定时任务**: 定时拉取订阅源更新，自动刷新 Mihomo 配置

## Conventions

- 所有代码注释、提交信息、文档使用简体中文
- Maven 标准目录结构：`src/main/java`, `src/test/java`, `src/main/resources`
