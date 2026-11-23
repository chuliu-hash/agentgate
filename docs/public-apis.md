# 真实天气与汇率接入

应用启动时按 code 补齐两条 PUBLISHED API。已存在时不覆盖管理员修改；不会在启动时访问外网。原 weather、order、user Mock 保留，用于离线联调、错误请求验证与脱敏观察。

| 编码 | 上游 | 请求参数 |
| --- | --- | --- |
| weather_live | https://api.open-meteo.com/v1/forecast | latitude、longitude、current=temperature_2m |
| exchange_rate | https://api.frankfurter.dev/v1/latest | base=USD、symbols=CNY,EUR |

## 操作顺序

1. 重启 Spring Boot，在 API 列表确认新增记录。无需清库或重新建表。
2. 开发者选择自己的应用申请相应 API，管理员审批；不要假设 API 的数据库 ID。
3. 在线调用选择请求示例，填写 AK/SK 后发送；选择示例不会自动调用，也不会替换凭据。
4. 或导入更新后的 [Apifox 文件](apifox/agentgate.openapi.json)，给真实 API 请求安装 [签名前置脚本](apifox/sign-request.js)。

```text
/openapi/weather_live?latitude=39.90&longitude=116.40&current=temperature_2m&timezone=Asia%2FShanghai
/openapi/exchange_rate?base=USD&symbols=CNY,EUR
```

天气使用经纬度，不能传 Mock 的 city 参数。检查天气响应中的 latitude、longitude、current.temperature_2m；汇率检查 base、date、rates。平台直接返回上游结构，不统一包装 success/code。

## 来源、限制及故障

天气来源为 Open-Meteo，免费接口面向非商业用途，使用时遵循官方服务条款、限额及数据署名要求；发布商业服务前重新核对授权。汇率来源为 Frankfurter，数据是参考汇率而非实时交易报价；节假日可能沿用最近可用日期。服务商条款和接口可用性可能变化，以其官方说明为准。

官方说明：

- Open-Meteo：https://open-meteo.com/en/docs 、https://open-meteo.com/en/pricing
- Frankfurter：https://frankfurter.dev/

两个上游均不需第三方密钥，但调用平台仍必须进行 AK/SK 验签、订阅和配额检查。平台限额按应用/API 计算，不统计同一供应商跨应用总消耗，应自行控制整体调用量。

上游调用使用 RestTemplate 直连，不继承系统代理。无法联网、超时、DNS 错误或上游限流时返回真实错误，不回退为 Mock。现有连接/读取超时为 3 秒/5 秒，失败请求已扣额度不退还。不要为解决网络问题关闭证书或 SSRF 校验。
