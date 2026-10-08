# Windows 部署说明

1. 在项目根目录运行 `.\build.ps1`，生成 `target\city-tour-monopoly-0.1.0.jar`。
2. 下载 WinSW，并将 EXE 与 `deploy\winsw\city-tour-service.xml` 放在同一目录。
3. 修改 XML 中的 JAR 绝对路径和 workingdirectory。
4. 管理员 PowerShell 执行：`.\city-tour-service.exe install`，然后 `Start-Service city-tour-monopoly`。
5. 安装 Caddy，将 `deploy\caddy\Caddyfile` 放到 Caddy 配置目录，并设置环境变量 `DOMAIN`。
6. 若使用公网域名，将路由器 80/443 转发到服务器，并确认 Windows 防火墙允许 Caddy。

建议让 Spring Boot 保持监听 `127.0.0.1:8080`，只通过 Caddy 暴露公网入口。Caddy 会自动处理 HTTPS 和 WebSocket Upgrade。