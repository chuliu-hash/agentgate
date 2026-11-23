'use strict';
const $ = id => document.getElementById(id);
const state = {token: sessionStorage.getItem('agentgate.token') || '', user: null, tab: '', apps: [], appId: '', page: 1, secret: null, generation: 0};
const adminTabs = {dashboard:'平台看板', apis:'API 管理', plans:'配额套餐', subscriptions:'订阅审批', logs:'调用日志', audit:'操作审计', runner:'在线调用'};
const devTabs = {dashboard:'应用看板', apps:'我的应用', apis:'API 目录', subscriptions:'我的订阅', logs:'调用日志', runner:'在线调用'};
const hints = {dashboard:'查看今日调用量及服务运行情况。', apps:'创建应用取得 AK/SK，然后申请 API 订阅。', apis:'管理接口定义或浏览已上架的工具。', plans:'配置每秒请求数及日/月额度。', subscriptions:'开发者申请后，需要管理员审批才能调用。', logs:'使用应用 ID、API ID 或 traceId 筛选调用记录。', audit:'查看管理员写操作的审计记录。', runner:'输入应用凭据，浏览器生成签名并发起真实调用。'};
const labels = {id:'ID', name:'名称', code:'编码', method:'方法', status:'状态', appKey:'AppKey', ipWhitelist:'IP 白名单', userId:'用户 ID', appId:'应用 ID',callCount:'调用次数',apiName:'API 名称', apiId:'API ID', quotaPlanId:'套餐 ID', rejectReason:'拒绝原因', qpsLimit:'QPS', dailyQuota:'日配额', monthlyQuota:'月配额', upstreamUrl:'上游地址', sensitivityLevel:'敏感级别', version:'版本', createTime:'创建时间', traceId:'Trace ID', path:'路径', statusCode:'HTTP 状态', latencyMs:'耗时 ms', errorCode:'错误码', clientIp:'客户端 IP', username:'操作人', action:'动作', resourceType:'资源类型', detail:'详情', ip:'IP'};
const apiFields = [['code','API 编码'],['name','名称'],['method','方法','select',['GET','POST','PUT','DELETE','ANY']],['upstreamUrl','上游 URL'],['version','版本'],['sensitivityLevel','敏感级别','number'],['description','说明','textarea']];
const planFields = [['name','名称'],['qpsLimit','QPS','number'],['dailyQuota','日配额','number'],['monthlyQuota','月配额','number'],['description','说明','textarea']];
function element(tag, text, cls) { const e=document.createElement(tag); if(text!==undefined)e.textContent=text; if(cls)e.className=cls; return e; }
let noticeTimer;
function clearNotice() {
 clearTimeout(noticeTimer);
 $('notice').textContent='';
 $('notice').className='';
 $('notice').style.display='none';
}
function notify(message, error=false) {
 clearNotice();
 $('notice').textContent=message;
 $('notice').className=error?'error':'';
 $('notice').style.display='block';
 if (!error) noticeTimer=setTimeout(clearNotice, 4000);
}
// 新操作开始即清除旧提示，避免上一次失败被误认为当前操作的结果。
// 使用捕获阶段，确保先清除旧内容，再由操作回调显示新的结果。
for (const eventName of ['click', 'submit', 'change']) {
 document.addEventListener(eventName, event => {
  if (eventName==='click' && !event.target.closest('button')) return;
  clearNotice();
  $('editor-error').textContent='';
 }, true);
}
function report(error) { notify(error.message || String(error),true); }
function button(text, callback, cls='secondary') { const b=element('button',text,cls); b.type='button'; b.onclick=async()=>{b.disabled=true;try{await callback();}catch(e){report(e);}finally{b.disabled=false;}}; return b; }
function showResponse(method,path,status,data,trace) { $('response').textContent=JSON.stringify({request:method+' '+path,status,traceId:trace||undefined,response:data},(key,value)=>key==='appSecret'?'[仅在凭据弹窗显示]':value,2); }
async function request(path,method='GET',body) {
 const headers={}; if(state.token)headers.Authorization='Bearer '+state.token; if(body!==undefined)headers['Content-Type']='application/json';
 const r=await fetch('/api'+path,{method,headers,body:body===undefined?undefined:JSON.stringify(body)});
 const raw=await r.text(); let data;try{data=JSON.parse(raw);}catch{data=raw;}
 showResponse(method,path,r.status,data,r.headers.get('X-Trace-Id'));
 if(!r.ok || (data && data.success===false)){if(r.status===401 && state.user)clearSession(); throw new Error((data&&data.message)||'请求失败：HTTP '+r.status);}
 return data && typeof data==='object' && 'success' in data ? data.data : data;
}
function clearSession(){state.token='';state.user=null;state.secret=null;state.apps=[];sessionStorage.removeItem('agentgate.token');$('workspace').hidden=true;$('auth').hidden=false;$('identity').textContent='未登录';document.getElementById("response-panel").hidden=true;$('response').textContent='尚未发送请求';$('content').replaceChildren();$('secret-value').textContent='';if($('secret-dialog').open)$('secret-dialog').close();}
let registering=false;
$('auth-toggle').onclick=()=>{registering=!registering;$('nickname-field').hidden=!registering;$('auth-submit').textContent=registering?'注册':'登录';$('auth-toggle').textContent=registering?'已有账号，去登录':'注册开发者账号';};
$('auth-form').onsubmit=async e=>{e.preventDefault();const submit=$('auth-submit');submit.disabled=true;try{const data=Object.fromEntries(new FormData(e.target));if(registering){await request('/user/register','POST',data);registering=false;$('nickname-field').hidden=true;submit.textContent='登录';$('auth-toggle').textContent='注册开发者账号';notify('注册成功，请登录。');}else{const result=await request('/user/login','POST',data);state.token=result.token;sessionStorage.setItem('agentgate.token',state.token);state.user=result.userDTO||result.user||null;if(!state.user)throw new Error('登录响应缺少用户信息');await enter();}e.target.elements.password.value='';}catch(err){report(err);}finally{submit.disabled=false;}};
$('logout').onclick=async()=>{try{await request('/user/logout','POST');}catch(e){report(e);}finally{clearSession();notify('已退出登录。');}};
async function enter(){ $('auth').hidden=true;$('workspace').hidden=false;$('identity').textContent=(state.user.nickname||state.user.username)+' · '+(isAdmin()?'管理员':'开发者');$('nav').replaceChildren();const tabs=isAdmin()?adminTabs:devTabs;for(const [key,title] of Object.entries(tabs)){$('nav').append(button(title,()=>openTab(key)));$('nav').lastChild.dataset.tab=key;}if(!isAdmin()){state.apps=await request('/developer/apps')||[];state.appId=String(state.apps[0]?.id||'');}await openTab('dashboard');}
function isAdmin(){return state.user?.role==='ADMIN';}
async function openTab(tab){state.tab=tab;state.page=1;await render();}
$('refresh').onclick=()=>render().catch(report);
function appSelector(parent){const label=element('label','当前应用');const select=element('select');for(const app of state.apps){const o=element('option',app.name+' #'+app.id);o.value=app.id;select.append(o);}select.value=state.appId;select.onchange=()=>{state.appId=select.value;state.page=1;render().catch(report);};label.append(select);parent.append(label);}
function table(rows,columns,actions,parent=$('content')){if(!rows?.length){parent.append(element('div','暂无数据','empty'));return;}const wrap=element('div',undefined,'table-wrap'),t=element('table'),head=element('tr');for(const col of columns)head.append(element('th',labels[col]||col));if(actions)head.append(element('th','操作'));const thead=element('thead');thead.append(head);t.append(thead);const tbody=element('tbody');for(const row of rows){const tr=element('tr');for(const col of columns){const val=row[col];const td=element('td',val===null||val===undefined?'—':typeof val==='object'?JSON.stringify(val):String(val));td.title=td.textContent;tr.append(td);}if(actions){const td=element('td');actions(row,td);tr.append(td);}tbody.append(tr);}t.append(tbody);wrap.append(t);parent.append(wrap);}
function addField(parent,[name,title,type='text',options],value){const label=element('label',title);const input=element(type==='select'?'select':type==='textarea'?'textarea':'input');input.name=name;if(input.tagName==='INPUT')input.type=type;if(options)for(const item of options){const o=element('option',typeof item==='object'?item.label:item);o.value=typeof item==='object'?item.value:item;input.append(o);}input.value=value??'';if(type==='number'){input.min='1';input.step='1';}if(!['description','nickname','reason','ipWhitelist'].includes(name))input.required=true;label.append(input);parent.append(label);return input;}
function edit(title,fields,values,save){$('editor-title').textContent=title;$('editor-error').textContent='';$('editor-fields').replaceChildren();for(const f of fields)addField($('editor-fields'),f,values[f[0]]);$('editor-form').onsubmit=async e=>{e.preventDefault();const submit=e.target.querySelector('[type=submit]');submit.disabled=true;try{const data=Object.fromEntries(new FormData(e.target));for(const f of fields)if(f[2]==='number')data[f[0]]=Number(data[f[0]]);await save(data);$('editor').close();notify('操作成功。');await render();}catch(err){$('editor-error').textContent=err.message;}finally{submit.disabled=false;}};$('editor').showModal();}
$('cancel-editor').onclick=()=>$('editor').close();
function showSecret(data){state.secret=data;$('secret-value').textContent=JSON.stringify(data,null,2);$('secret-dialog').showModal();}
$('close-secret').onclick=()=>{state.secret=null;$('secret-value').textContent='';$('secret-dialog').close();};
$('secret-dialog').addEventListener('cancel',()=>{state.secret=null;$('secret-value').textContent='';});
$('use-secret').onclick=async()=>{$('secret-dialog').close();await openTab('runner');};
function appDisableButton(app) {
 const disabled = app.status === 'DISABLED';
 const control = button(disabled ? '已禁用' : '禁用', async () => {
  if (confirm('确认禁用此应用？')) await mutate('/developer/apps/' + app.id + '/disable');
 }, disabled ? 'secondary' : 'danger');
 control.disabled = disabled;
 return control;
}
async function mutate(path,method='POST',body){await request(path,method,body);notify('操作成功。');await render();}
async function render(){
 const tab=state.tab, generation=++state.generation; document.getElementById("response-panel").hidden=tab!=='runner'; document.getElementById("response-panel").open=false;const root=$('content'),controls=$('controls');root.replaceChildren();controls.replaceChildren();$('title').textContent=(isAdmin()?adminTabs:devTabs)[tab];$('description').textContent=hints[tab];document.querySelectorAll('#nav button').forEach(b=>b.classList.toggle('active',b.dataset.tab===tab));
 try{
 if(tab==='runner'){runner();return;}
 if(!isAdmin()&&['dashboard','subscriptions','logs'].includes(tab)){state.apps=await request('/developer/apps')||[];if(generation!==state.generation)return;if(!state.apps.some(a=>String(a.id)===state.appId))state.appId=String(state.apps[0]?.id||'');appSelector(controls);if(!state.appId){root.append(element('div','请先在“我的应用”创建应用。','empty'));return;}}
 const prefix=isAdmin()?'/admin':'/developer';
 if(tab==='dashboard'){const data=await request(prefix+'/dashboard'+(isAdmin()?'':'?appId='+state.appId));if(generation!==state.generation)return;const stats=element('div',undefined,'stats');for(const [key,title] of Object.entries({todayCalls:'今日调用',successCalls:'成功调用',successRate:'成功率 %',avgLatencyMs:'平均耗时 ms',activeApps:'活跃应用'})){const card=element('div',title,'stat');card.append(element('strong',typeof data[key]==='number'?String(Math.round(data[key]*100)/100):'—'));stats.append(card);}root.append(stats,element('h2','热门 API'));table(data.topApis,['apiId','apiName','callCount']);}
 if(tab==='apps'){controls.append(button('创建应用',()=>edit('创建应用',[['name','应用名称'],['ipWhitelist','IP 白名单']],{ipWhitelist:'*'},async data=>{showSecret(await request('/developer/apps','POST',data));})));const rows=await request('/developer/apps');if(generation!==state.generation)return;state.apps=rows;table(rows,['id','name','appKey','status','ipWhitelist'],(r,td)=>{td.append(button('白名单',()=>edit('修改白名单',[['ipWhitelist','IP 白名单']],r,data=>request('/developer/apps/'+r.id+'/ip-whitelist','PUT',data))),button('重置密钥',async()=>{if(confirm('重置后旧 Secret 将失效，继续？'))showSecret(await request('/developer/apps/'+r.id+'/reset-secret','POST'));}),appDisableButton(r));});}
 if(tab==='apis'){if(isAdmin())controls.append(button('新增 API',()=>edit('新增 API',apiFields,{method:'GET',version:'v1',sensitivityLevel:1,timeoutMs:5000},data=>request('/admin/apis','POST',data))));const rows=await request(prefix+'/apis');if(generation!==state.generation)return;table(rows,['id','code','name','method','status','sensitivityLevel','upstreamUrl'],isAdmin()?(r,td)=>{td.append(button('编辑',()=>edit('编辑 API',apiFields,r,data=>request('/admin/apis/'+r.id,'PUT',data))),button(r.status==='PUBLISHED'?'下架':'上架',async()=>{const action=r.status==='PUBLISHED'?'offline':'publish';if(confirm('确认'+(action==='offline'?'下架':'上架')+'此 API？'))await mutate('/admin/apis/'+r.id+'/'+action);}));}:null);}
 if(tab==='plans'){controls.append(button('新增套餐',()=>edit('新增套餐',planFields,{qpsLimit:2,dailyQuota:1000,monthlyQuota:10000},data=>request('/admin/quota-plans','POST',data))));const rows=await request('/admin/quota-plans');if(generation!==state.generation)return;table(rows,['id','name','qpsLimit','dailyQuota','monthlyQuota'],(r,td)=>td.append(button('编辑',()=>edit('编辑套餐',planFields,r,data=>request('/admin/quota-plans/'+r.id,'PUT',data)))));}
 if(tab==='subscriptions'){if(!isAdmin())controls.append(button('申请订阅',async()=>{const appId=state.appId;const apis=await request('/developer/apis');if(!apis.length)throw new Error('当前没有已上架 API');edit('申请订阅',[['apiId','API','select',apis.map(api=>({value:String(api.id),label:api.name+' ('+api.code+')'}))],['quotaPlanId','套餐 ID','number']],{apiId:apis[0].id,quotaPlanId:1},data=>request('/developer/subscriptions','POST',{...data,apiId:Number(data.apiId),appId:Number(appId)}));}));const rows=await request(prefix+'/subscriptions'+(isAdmin()?'':'?appId='+state.appId));if(generation!==state.generation)return;table(rows,['id','appId','apiId','quotaPlanId','status','rejectReason'],isAdmin()?(r,td)=>{if(r.status==='PENDING')td.append(button('通过',async()=>{if(confirm('确认通过此订阅？'))await mutate('/admin/subscriptions/'+r.id+'/approve','POST',{approved:true});}),button('拒绝',()=>edit('拒绝订阅',[['reason','拒绝原因','textarea']],{},data=>request('/admin/subscriptions/'+r.id+'/approve','POST',{approved:false,...data})),'danger'));}:null);}
 if(tab==='logs'||tab==='audit')await logs(prefix,tab,generation);
 }catch(e){if(generation===state.generation){root.append(element('div',e.message,'empty'));report(e);}}
}
const logFilters={traceId:'',apiId:''};
async function logs(prefix,tab,generation){const audit=tab==='audit',controls=$('controls');const params=new URLSearchParams({page:state.page,size:10});if(!audit){if(!isAdmin())params.set('appId',state.appId);const fields=element('div',undefined,'fields');for(const [key,title] of [['traceId','Trace ID'],['apiId','API ID']]){const input=addField(fields,[key,title],logFilters[key]);input.required=false;input.onchange=()=>{logFilters[key]=input.value.trim();};if(logFilters[key])params.set(key,logFilters[key]);}controls.append(fields,button('查询',async()=>{state.page=1;await render();}));}const data=await request(prefix+(audit?'/audit-logs':'/call-logs')+'?'+params);if(generation!==state.generation)return;table(data.records,audit?['id','username','action','resourceType','detail','ip','createTime']:['traceId','appId','apiId','method','path','statusCode','latencyMs','errorCode'],(row,td)=>td.append(button('详情',()=>{document.getElementById("response-panel").hidden=false;$('response').textContent=JSON.stringify(row,null,2);$('response').parentElement.open=true;})));const page=element('div',undefined,'pagination');const prev=button('上一页',async()=>{state.page--;await render();});prev.disabled=state.page<=1;const next=button('下一页',async()=>{state.page++;await render();});next.disabled=state.page*10>=data.total;page.append(prev,element('span','第 '+state.page+' 页 · 共 '+data.total+' 条'),next);$('content').append(page);}
function javaEncode(value){return encodeURIComponent(value).replace(/[!'()~]/g,c=>'%'+c.charCodeAt(0).toString(16).toUpperCase()).replace(/%20/g,'+');}
function canonicalQuery(query){const pairs=new Map();for(const pair of query.split('&')){if(!pair)continue;const i=pair.indexOf('=');const decode=s=>{try{return decodeURIComponent(s.replace(/\+/g,' '));}catch{return s;}};const k=decode(i<0?pair:pair.slice(0,i)),v=decode(i<0?'':pair.slice(i+1));if(!pairs.has(k))pairs.set(k,[]);pairs.get(k).push(v);}return [...pairs.keys()].sort().flatMap(k=>pairs.get(k).sort().map(v=>javaEncode(k)+'='+javaEncode(v))).join('&');}
const hex=buffer=>Array.from(new Uint8Array(buffer),b=>b.toString(16).padStart(2,'0')).join('');
function runner(){const form=element('form'),fields=element('div',undefined,'fields');
const presetLabel=element('label','请求示例');
const preset=element('select');
const samples=[['自定义',''],['真实天气 · Open-Meteo','/openapi/weather_live?latitude=39.90&longitude=116.40&current=temperature_2m&timezone=Asia%2FShanghai'],['参考汇率 · Frankfurter','/openapi/exchange_rate?base=USD&symbols=CNY,EUR'],['Mock 天气','/openapi/weather?city=beijing'],['Mock 订单脱敏','/openapi/order/1001'],['Mock 用户脱敏','/openapi/user/1001']];
for(const [title,path] of samples){const option=element('option',title);option.value=path;preset.append(option);}
preset.onchange=()=>{if(preset.value){form.elements.method.value='GET';form.elements.path.value=preset.value;form.elements.body.value='';}};
presetLabel.append(preset);form.append(presetLabel);
const sourceNote=element('p','天气数据来源 Open-Meteo（免费层限非商业用途）；汇率来源 Frankfurter，为参考汇率。使用前需申请对应 API 订阅。','muted');form.append(sourceNote);const defaults={appKey:state.secret?.appKey||'',appSecret:state.secret?.appSecret||'',method:'GET',path:'/openapi/weather?city=beijing',body:''};for(const f of [['appKey','AppKey'],['appSecret','AppSecret','password'],['method','方法','select',['GET','POST','PUT','DELETE']],['path','调用路径（含查询参数）'],['body','请求体（原始内容，可留空）','textarea']]){const input=addField(fields,f,defaults[f[0]]);if(f[0]==='body')input.required=false;if(f[0]==='appSecret')input.autocomplete='off';}state.secret=null;$('secret-value').textContent='';form.append(fields);const submit=element('button','生成签名并调用');submit.type='submit';form.append(submit);form.onsubmit=async e=>{e.preventDefault();submit.disabled=true;try{if(!globalThis.crypto?.subtle)throw new Error('浏览器签名需要 localhost 或 HTTPS 安全上下文。');const d=Object.fromEntries(new FormData(form));if(!d.path.startsWith('/openapi/')||d.path.includes('#'))throw new Error('请输入 /openapi/ 开头且不含 # 的路径');const url=new URL(d.path,location.origin);if(url.origin!==location.origin||!url.pathname.startsWith('/openapi/'))throw new Error('只允许本平台开放接口路径');if(d.body&&d.method==='GET')throw new Error('GET 请求不能携带请求体');const enc=new TextEncoder(),body=enc.encode(d.body),ts=String(Math.floor(Date.now()/1000)),nonce=crypto.randomUUID();const canonical=[d.method,url.pathname,canonicalQuery(url.search.slice(1)),ts,nonce,hex(await crypto.subtle.digest('SHA-256',body))].join('\n');const key=await crypto.subtle.importKey('raw',enc.encode(d.appSecret),{name:'HMAC',hash:'SHA-256'},false,['sign']);const signature=hex(await crypto.subtle.sign('HMAC',key,enc.encode(canonical)));const r=await fetch('/api'+url.pathname+url.search,{method:d.method,headers:{'Content-Type':'application/json','X-App-Key':d.appKey,'X-Timestamp':ts,'X-Nonce':nonce,'X-Signature':signature},body:d.method==='GET'?undefined:d.body});const text=await r.text();let response;try{response=JSON.parse(text);}catch{response=text;}showResponse(d.method,url.pathname+url.search,r.status,response,r.headers.get('X-Trace-Id'));$('response').parentElement.open=true;notify('调用完成：HTTP '+r.status,!r.ok);}catch(err){report(err);}finally{submit.disabled=false;}};$('content').append(form);}
if(state.token){request('/user/me').then(async user=>{state.user=user;await enter();}).catch(e=>{clearSession();report(e);});}
