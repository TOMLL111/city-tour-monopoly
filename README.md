# 城市环游记

3–7 人 Web 联机大富翁风格游戏。后端使用 Java 17 + Spring Boot，前端使用 React + TypeScript + Vite，浏览器通过 REST 创建/加入房间，通过原生 WebSocket 接收服务端权威状态。

## 已实现功能

- 6 位房间码、游客昵称、3–7 人限制、准备与房主开局
- 40 格原创城市旅行棋盘
- 买地、收租、同色垄断双倍租金、四条交通枢纽、两个公共设施
- 建房、卖房、抵押、赎回和均衡建房规则
- 玩家交易：地产、现金和出狱卡
- 拍卖、监狱、机会/命运卡、税务、债务和破产结算
- 掉线暂停、会话令牌重连、主动退出视为破产
- 事件时间线、文字聊天和 8 个快捷表情
- 桌面优先界面，手机端可查看棋盘与完成主要操作

## 本地开发

需要 Java 17、Node.js 24 和 npm。项目自带 Maven Wrapper。

```powershell
# 终端 1：后端
.\mvnw.cmd -DskipFrontend=true spring-boot:run

# 终端 2：前端开发服务器
cd frontend
npm.cmd install
npm.cmd run dev
```

浏览器打开 `http://127.0.0.1:5173`。Vite 会将 `/api` 和 `/ws` 代理到 `127.0.0.1:8080`。

## 测试与构建

```powershell
# 后端测试
.\mvnw.cmd -DskipFrontend=true test

# 前端类型检查与生产构建
cd frontend
npm.cmd run build

# 完整打包：会执行 npm ci、前端构建和后端测试
cd ..
.\mvnw.cmd clean package
```

完整 JAR 位于 `target/city-tour-monopoly-0.1.0.jar`。JAR 已包含前端静态资源和 WebSocket 服务。

## 运行生产包

```powershell
java -jar target\city-tour-monopoly-0.1.0.jar
```

默认仅监听 `127.0.0.1:8080`。使用以下命令可通过局域网访问：

```powershell
java -jar target\city-tour-monopoly-0.1.0.jar --server.address=0.0.0.0
```

## Windows 部署

1. 完整构建 JAR。
2. 按 `deploy/winsw/city-tour-service.xml` 修改绝对路径，使用 WinSW 注册为 Windows 服务。
3. 使用 `deploy/caddy/Caddyfile` 配置 HTTPS 和 WebSocket 反向代理。
4. 有公网域名时，将路由器 80/443 端口转发到 Windows 主机。

对局只保存在服务器内存中，服务重启会丢失未结束对局；这是首版的设计边界。

## 配置

- `server.port`：默认 `8080`
- `server.address`：默认 `127.0.0.1`，局域网部署改为 `0.0.0.0`
- `management.endpoints.web.exposure.include`：健康检查与信息端点