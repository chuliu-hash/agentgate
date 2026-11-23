// 粘贴到开放调用接口的 Apifox 前置脚本。使用 pm 兼容脚本 API。
// 环境变量：appKey、appSecret；base URL 可直连 8081 或经 8088/api 代理。
const CryptoJS = require('crypto-js');
const resolve = value => pm.variables.replaceIn(value);
const key = pm.environment.get('appKey');
const secret = pm.environment.get('appSecret');
if (!key || !secret) throw new Error('请设置环境变量 appKey 和 appSecret');
const url = new URL(resolve(pm.request.url.toString()));
const path = url.pathname.startsWith('/api/openapi/') ? url.pathname.slice(4) : url.pathname;
if (!path.startsWith('/openapi/')) throw new Error('仅用于 /openapi/ 接口');
const encode = value => encodeURIComponent(value)
    .replace(/[!'()~]/g, c => '%' + c.charCodeAt(0).toString(16).toUpperCase())
    .replace(/%20/g, '+');
const groups = new Map();
for (const [k, v] of url.searchParams) {
    if (!groups.has(k)) groups.set(k, []);
    groups.get(k).push(v);
}
const query = [...groups.keys()].sort().flatMap(k =>
    groups.get(k).sort().map(v => encode(k) + '=' + encode(v))).join('&');
if (pm.request.body && !['raw', ''].includes(pm.request.body.mode || '')) {
    throw new Error('签名脚本只支持空请求体或 raw 请求体');
}
const body = pm.request.body?.mode === 'raw' ? resolve(pm.request.body.raw || '') : '';
const timestamp = String(Math.floor(Date.now() / 1000));
const nonce = CryptoJS.lib.WordArray.random(16).toString();
const canonical = [pm.request.method.toUpperCase(), path, query, timestamp, nonce,
    CryptoJS.SHA256(body).toString()].join('\n');
const signature = CryptoJS.HmacSHA256(canonical, secret).toString();
for (const [name, value] of Object.entries({
    'X-App-Key': key, 'X-Timestamp': timestamp, 'X-Nonce': nonce, 'X-Signature': signature
})) pm.request.headers.upsert({key: name, value});
