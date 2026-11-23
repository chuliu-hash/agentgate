# Apifox 接口导入与测试

已补充 weather_live、exchange_rate 两个具体 GET 请求，详见 [真实 API 使用说明](../public-apis.md)。当前导入文件共 42 个操作，以下原有 40 个操作说明不包含这两个新增请求。

## 导入

在 Apifox 项目中选择“导入数据”，使用 OpenAPI/Swagger 文件导入本目录的 `agentgate.openapi.json`。文件采用 OpenAPI 3.0.3、UTF-8，无需启动后端即可导入。包含 40 个操作：29 个管理/开发者/用户操作、8 个开放调用模板、3 个模拟上游接口。

服务地址选择 `http://localhost:8081`，或经 Nginx 使用 `http://localhost:8088/api`。不要重复添加 `/api` 前缀。启动依赖见 [Docker 服务](../docker-services.md)，完整接口说明见 [API 文档](../api.md)。

## 账号与变量

分别保存管理员和开发者 Token。在接口的 Bearer Token 认证中填写对应 Token；登录、注册、开放调用和 Mock 接口无需用户 Token。管理员接口必须使用 ADMIN 身份，不能只替换 appId 绕过权限。

建议环境变量：`devToken`、`adminToken`、`appId`、`apiId`、`quotaPlanId`、`subscriptionId`、`appKey`、`appSecret`。创建或重置返回的 Secret 及时保存到本地环境变量，不要共享含真实凭据的环境。

登录返回 `data.token` 和 `data.userDTO`，不是 `data.user`。可在开发者登录接口后置脚本提取：

```javascript
const body = pm.response.json();
pm.test('登录成功', () => pm.expect(body.success).to.eql(true));
if (body.success) pm.environment.set('devToken', body.data.token);
```

管理员登录将变量名改为 `adminToken`。创建应用成功后可提取：

```javascript
const body = pm.response.json();
if (body.success) {
    for (const name of ['appId', 'appKey', 'appSecret']) {
        pm.environment.set(name, body.data[name]);
    }
}
```

标准 OpenAPI 不承载 Apifox 专用环境及脚本配置，导入后需要手动填写认证、变量和脚本。签名脚本采用 pm 兼容 API；应在 Apifox 中执行一次确认当前客户端支持其脚本运行环境。

## 建议测试顺序

1. 注册开发者，登录并保存 Token；管理员独立登录。
2. 开发者创建应用，保存实际 appId、AK/SK。
3. 查已上架 API 和管理员套餐列表，保存真实 apiId、quotaPlanId，不要假定示例 ID 一定存在。
4. 开发者申请订阅，保存返回 id；管理员审批通过。
5. 开放接口选择 GET，apiCode 填 weather，city 填 beijing。将 [sign-request.js](sign-request.js) 粘贴到该请求的前置脚本，设置 appKey/appSecret，然后发送。
6. 订阅 order 后使用带 resourceId 的调用模板，apiCode=order、resourceId=1001，观察敏感字段掩码。
7. 稍后查调用日志、看板；管理员写操作后查审计。MQ 异步消费可能有延迟。
8. 密钥重置、API 下架、应用禁用放在正常流程之后，避免影响后续测试。

开放入口支持更多 HTTP 方法及多级路径，导入文件用 GET/POST/PUT/DELETE 和单个 resourceId 展示常用模板，不表示后端仅支持这些路径。API 自身登记的方法仍会限制调用。GET 留空 body；POST/PUT/DELETE 使用 raw 内容并对实际发送字节签名。

## 验证用例

| 场景 | 预期 |
| --- | --- |
| 正常管理操作 | HTTP 200，success=true，code=0 |
| 未登录访问 /user/me | HTTP 401 |
| 开发者访问 /admin/apis | HTTP 403 |
| 注册用户名少于 3 字符 | HTTP 400 |
| 无四个签名头 | HTTP 401，code=1001 |
| 错误签名 | HTTP 401，code=1004 |
| 过期时间戳且不运行自动脚本 | HTTP 401，code=1002 |
| 原样重发同一组已成功签名头 | HTTP 401，code=1003；暂停前置脚本，否则它会生成新 Nonce |
| 未订阅已发布 API | HTTP 403，code=1008 |
| API 下架/应用禁用 | HTTP 403，code=1007/1005 |
| 连续超出 QPS | HTTP 429，code=429 |
| 超过日/月配额 | HTTP 429，code=4290 |

开放调用成功时直接返回上游响应，不能统一断言 success/code；错误也可能是上游原始响应。失败记录可能没有 appId/apiId，可用管理员日志检查。
