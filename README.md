# LangChain4j AI Agent Demo

基于Spring Boot和LangChain4j的AI智能体演示项目，集成了DeepSeek API、Chroma向量数据库和多种工具，实现了动态系统提示词管理和AI对话功能。

## 🌟 功能特性

- 🤖 **AI对话系统**: 基于DeepSeek大语言模型的智能对话
- 🔧 **工具集成**: 集成多种工具，包括网络搜索、时间查询等
- 🗃️ **向量数据库**: 使用Chroma向量数据库存储和检索对话历史
- 📝 **动态提示词**: 支持动态管理系统提示词
- 🌐 **RESTful API**: 提供完整的REST API接口
- 📊 **日志记录**: 详细的请求和响应日志

## 🛠️ 技术栈

- **后端框架**: Spring Boot 3.x
- **AI框架**: LangChain4j
- **语言模型**: DeepSeek API
- **向量数据库**: Chroma
- **构建工具**: Maven
- **开发语言**: Java 17+

## 📁 项目结构

```
langchain4j-ai-agent-demo/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/langchain4jdeepseek/
│       │       ├── config/          # 配置类
│       │       ├── controller/      # REST控制器
│       │       ├── model/           # 数据模型
│       │       ├── service/         # 业务逻辑
│       │       └── tools/           # AI工具类
│       └── resources/
│           ├── application.properties      # 应用配置
│           ├── application-local.properties # 本地API密钥配置(不提交到版本控制)
│           ├── command-blacklist.txt      # 命令执行黑名单配置
│           └── prompts/                   # 系统提示词模板
├── start.sh                      # 统一启动脚本
├── test-all-features.sh          # 功能测试脚本
├── pom.xml                       # Maven配置
└── README.md                     # 项目说明
```

## 🚀 快速开始

### 本地开发

1. 克隆仓库：
   ```bash
   git clone https://github.com/HeroJY/langchain4j-ai-agent-demo.git
   cd langchain4j-ai-agent-demo
   ```

2. 配置API密钥：
   ```bash
   cp src/main/resources/application.properties src/main/resources/application-local.properties
   ```
   然后编辑 `application-local.properties` 文件，添加您的实际API密钥：
   ```properties
   deepseek.api.key=您的实际DeepSeek API密钥
   tavily.api.key=您的实际Tavily API密钥
   ```

3. 启动应用（使用本地配置文件）：
   ```bash
   # 使用统一的启动脚本（默认使用Spring Profile方式）
   ./start.sh
   
   # 或者直接指定配置文件方式
   ./start.sh --config
   ```

4. 访问应用：
   打开浏览器访问 http://localhost:8080

### 启动脚本详细说明

项目提供了统一的启动脚本 `start.sh`，支持两种启动方式：

#### 方法一：使用Spring Profile（推荐）
```bash
# 使用默认方式启动（Spring Profile）
./start.sh

# 或者明确指定使用Spring Profile方式
./start.sh --profile
```

当设置 `SPRING_PROFILES_ACTIVE=local` 时，Spring Boot会自动加载 `application-local.properties` 文件。

#### 方法二：直接指定配置文件位置
```bash
# 直接指定配置文件方式启动
./start.sh --config
```

这种方式通过命令行参数直接指定配置文件的位置。

配置文件的加载顺序如下：
1. 首先加载 `application.properties`（基础配置）
2. 然后加载 `application-local.properties`（会覆盖相同的配置项）

## 📖 API文档

### 健康检查

```http
GET /health
```

### AI对话

```http
POST /api/ai/chat
Content-Type: application/json

{
  "message": "你好，请介绍一下你自己",
  "userId": "user123"
}
```

### 更新系统提示词

```http
POST /api/prompts/system
Content-Type: application/json

{
  "prompt": "你是一个专业的AI助手，请用简洁明了的语言回答问题。"
}
```

### 获取当前系统提示词

```http
GET /api/prompts/system
```

## 🗃️ 向量数据库集成

项目集成了Chroma向量数据库用于存储和检索对话历史。向量数据库的配置和初始化在应用启动时自动完成，无需手动操作。

## 🔧 工具集成

项目集成了多种AI工具，扩展了AI助手的能力：

- **时间工具**: 获取当前时间和日期
- **计算工具**: 执行基本数学计算
- **搜索工具**: 使用Tavily API进行网络搜索

## 📝 系统提示词管理

系统提示词存储在 `src/main/resources/prompts/` 目录下：

- `system-prompt.txt`: 默认系统提示词
- `system-prompt-cn.txt`: 中文系统提示词

可以通过API动态更新系统提示词，无需重启应用。

## 🧪 测试

### 运行所有功能测试

```bash
./test-all-features.sh
```

## 🔒 安全特性

项目实现了多项安全措施来保护系统安全：

1. **命令执行安全**
   - 实现了命令黑名单机制，防止执行危险命令
   - 黑名单包含系统破坏、用户管理、网络配置等各类危险命令
   - 支持从配置文件动态加载黑名单列表

2. **API密钥管理**
   - 本地配置文件 `application-local.properties` 已被添加到 `.gitignore`
   - 不会将实际API密钥提交到版本控制系统
   - 支持通过环境变量或外部配置文件管理密钥

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进项目！

如果您想为此项目贡献代码，请确保：

1. 不要在代码中硬编码任何API密钥
2. 使用占位符代替实际的密钥值
3. 在提交代码前检查是否意外包含了敏感信息

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🙏 致谢

- [LangChain4j](https://github.com/langchain4j/langchain4j) - Java的LLM框架
- [DeepSeek](https://www.deepseek.com/) - 大语言模型API服务
- [Chroma](https://www.trychroma.com/) - 开源向量数据库
- [Tavily](https://tavily.com/) - 搜索API服务

---

⭐ 如果这个项目对您有帮助，请给它一个星标！