# 前后端部署与用户控制台

## 目录与访问链路

前端采用原生 HTML/CSS/JavaScript，没有 Node/npm 构建依赖、CDN 或额外框架，方便集中学习后端。文件位于 `nginx/html/`，由 Nginx 单独托管；Spring Boot 仍只提供接口。

```text
浏览器 http://localhost:8088/
  ├─ /index.html、/app.js、/style.css → nginx/html/
  └─ /api/user/login 等 → Nginx 去除 /api/ → 127.0.0.1:8081/user/login
```

浏览器只请求 Nginx 同源地址，无需修改后端 CORS 或增加静态资源放行规则。签名使用后端收到的 `/openapi/...` 路径，不包含代理前缀 `/api`。Nginx 默认端口选为 8088，避免和黑马点评的 8080 冲突。

## Windows 启动

先依照 [Docker 依赖服务](docker-services.md) 启动三个容器，再启动 Spring Boot（8081）。黑马点评后端也可能占用 8081，请先停止它或同时修改本项目后端端口和 `nginx/conf/nginx.conf` 中的 upstream 地址。

仓库不复制 Nginx 可执行文件。可复用已有的安装，只使用它的程序，不修改其配置或页面。以下命令在 AgentGate 根目录执行：

```powershell
$nginxExe = 'C:\Users\chuliu\ideaProject\heimadianping\nginx-1.18.0 dianping\nginx.exe'
powershell -NoProfile -ExecutionPolicy Bypass -File .\nginx\manage.ps1 -Action test -NginxExe $nginxExe
powershell -NoProfile -ExecutionPolicy Bypass -File .\nginx\manage.ps1 -Action start -NginxExe $nginxExe
```

打开 **http://localhost:8088/**。`manage.ps1` 使用当前项目的独立 prefix，自动创建 logs/temp，以隐藏窗口启动。若 nginx.exe 已加入 PATH，可省略 `-NginxExe`。不要重复 start；改配置后 reload。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\nginx\manage.ps1 -Action reload -NginxExe $nginxExe
powershell -NoProfile -ExecutionPolicy Bypass -File .\nginx\manage.ps1 -Action stop -NginxExe $nginxExe
```

Linux 使用自己的 Nginx 安装：在 `nginx/` 内创建 `logs`、`temp`，然后 `nginx -p "$PWD/" -c conf/nginx.conf -t`，检查通过后去掉 `-t` 启动。此配置仅供本地演示；服务器按自己的部署目录调整。

## 页面功能

| 身份 | 功能 |
| --- | --- |
| 未登录 | 开发者注册、登录 |
| 开发者 | 应用创建、白名单、密钥重置和禁用；API 目录；订阅申请及状态；应用看板和日志 |
| 管理员 | API 新增/编辑/上下架；套餐新增/编辑；订阅审批；全局看板、调用日志和管理审计 |
| 已登录用户 | 开放 API 签名调用，展示 HTTP 状态、响应体和 traceId |

页面中的 API 请求均连接真实后端，没有模拟业务数据。管理员菜单只做展示控制，真正的鉴权与所有权检查仍在后端。开发者申请套餐目前需输入管理员提供的套餐 ID，因为现有后端没有开发者套餐查询接口；前端不会绕过管理员权限查询它。

## 完整操作流程

1. 用普通窗口注册并登录开发者，创建应用，保存弹窗里的 AppKey/AppSecret。可选择“前往在线调用”将凭据带入当前页面内存中的表单。
2. 查看 API 目录，记下 weather 的 API ID。打开“我的订阅”，选择应用，填写 API ID 和管理员告知的套餐 ID 申请订阅。
3. 用另一个浏览器配置或无痕窗口登录管理员 `admin/admin123`，检查套餐 ID，在“订阅审批”通过申请。不要猜测初始化 ID。
4. 回开发者窗口确认订阅为 ACTIVE，在“在线调用”填 AK/SK 和 `/openapi/weather?city=beijing`，点击调用。每次自动生成时间戳、Nonce、body 哈希及 HMAC。
5. 订阅 order 后调用 `/openapi/order/1001` 观察脱敏；连续调用观察限流；稍后刷新日志与看板观察 MQ 异步入库结果。

## 凭据与浏览器行为

Token 仅保存在当前标签页 sessionStorage，刷新后通过 `/user/me` 恢复身份；退出或接口返回登录失效时清理。SK 不写入浏览器持久存储，关闭凭据弹窗或离开测试表单后需自行重新填写；调用结果面板不记录创建/重置返回的 SK。请勿将真实生产密钥用于公开演示。

签名使用浏览器 Web Crypto，需要 localhost 或 HTTPS，建议使用上面的 localhost 地址。请求体按输入原始字节签名，GET 不允许 body；Query 按 Java URLEncoder 规则排序编码，支持中文、空格与重复参数。该工具默认以 application/json 发送请求体。

## 排错与范围

- 502：后端未在 8081 启动，检查 Nginx error.log 和 Spring Boot 日志。
- 登录成功但空列表：真实数据库可能还没有应用、订阅或日志，先完成演示流程。
- 签名 401：检查 Secret 是否已重置、时钟、请求路径以及 Nonce。
- 订阅 403：检查审批、应用状态与 IP 白名单。默认后端不信任代理头，因此通常看到 Nginx 的源 IP；要测试真实客户端 IP，须正确配置可信代理后再启用 trust-proxy。
- 日志暂未出现：检查 MQ 消费；页面刷新不会伪造数据。

这是用于应用接入和管理的轻量用户控制台，不包含生产级前端权限体系、图表库或复杂组件框架。页面和后端可独立部署，代理地址和端口在 Nginx 配置中调整。
