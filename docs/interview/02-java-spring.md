# 2. Java 与 Spring 后端基础题

回答基础题时，先给出清晰定义，再讲关键机制，最后用项目例子落地。不要把八股背成孤立术语。

## 2.1 Java 集合与对象

### HashMap 的基本原理是什么？

HashMap 使用数组桶保存键值对，哈希值决定桶下标；发生冲突时通过链表或树结构保存多个节点。JDK 8 中桶内链表达到树化阈值时，在容量也满足条件的情况下可转成红黑树，降低严重冲突时的查找成本；扩容时重新分配桶位置。`hashCode()` 用于定位，`equals()` 用于确认键是否相同，所以作为 key 的类应遵守“相等对象必须有相同 hashCode”的约定。

### HashMap 为什么不适合多线程并发写？

HashMap 没有提供并发读写的同步语义，多线程同时修改可能造成数据竞争、覆盖和可见性问题。并发场景可使用 `ConcurrentHashMap`，或者在更高层用锁保护复合操作。即使换成 ConcurrentHashMap，“先判断再写入”这类多步业务逻辑也要用原子 API 或额外同步。

### ArrayList 与 LinkedList 怎么选？

ArrayList 基于动态数组，随机访问快，尾部追加摊销成本低，中间插入删除需要搬移元素；LinkedList 是双向链表，按下标访问慢，已拿到节点时局部插删较直接。多数业务列表以遍历和随机访问为主，通常优先 ArrayList。不要仅根据“插入删除多”就选链表，还要考虑查找节点本身的成本和对象内存开销。

### `==` 和 `equals()` 有什么区别？

基本类型的 `==` 比较值；引用类型的 `==` 比较是否为同一个对象。`equals()` 默认也是引用比较，但类可以覆盖它来定义业务相等。作为 HashMap key 时，`equals()`、`hashCode()` 必须保持一致，且参与相等判断的字段最好不要在放入 Map 后改变。

## 2.2 并发基础

### `synchronized` 和 `volatile` 区别？

`volatile` 保证对变量读写的可见性，并限制部分重排序，但不保证 `i++` 这类复合操作原子。`synchronized` 提供互斥和可见性，适合保护临界区。项目中的调用量不能靠 JVM 内的锁完成多实例共享计数，因此使用 Redis Lua 在 Redis 服务端执行限流脚本。

### 线程池核心参数怎么解释？

重点讲核心线程数、最大线程数、空闲线程存活时间、任务队列、线程工厂和拒绝策略。任务先由核心线程执行，核心线程忙时进入队列，队列满后才扩容到最大线程数，仍无法接收时执行拒绝策略。线上要结合任务耗时、下游容量、队列长度和拒绝次数监控；不能用无限队列掩盖过载。

如果项目被问到线程池：当前代码没有为调用日志配置独立的业务线程池，异步解耦由 RabbitMQ 完成；不要误说成 `@Async` 或自建线程池。

### 什么是幂等？

同一业务请求执行一次或多次，最终业务结果一致。常见实现包括唯一业务键、幂等表、状态条件更新、Redis 去重键。AgentGate 的调用日志以 `trace_id` 唯一索引加 `INSERT IGNORE` 防止重复投递时重复插入；统计更新仅在日志首次插入成功时执行。

## 2.3 JVM 与异常

### JVM 内存区域可以怎么说？

线程私有区域包括程序计数器、Java 虚拟机栈和本地方法栈；线程共享区域主要有堆和方法区实现（HotSpot 中对应元空间）。对象主要分配在堆上，栈帧保存局部变量、操作数栈和方法调用信息。排查内存问题时结合堆转储、GC 日志和对象引用链，而不是只看“堆很大”。

### 垃圾回收器怎么回答？

先说明对象可达性分析和分代/分区回收的目标，再根据 JDK 版本与服务延迟要求选择收集器。不要在没有运行参数和监控数据时声称本项目使用某个指定 GC；当前项目配置没有显式指定垃圾收集器。

### 受检异常与非受检异常的区别？

受检异常在编译期要求捕获或声明；`RuntimeException` 及其子类是非受检异常。业务校验失败在项目中通过 `BizException` 表达，再由 `WebExceptionAdvice` 等统一转换为 API 响应。事务方法使用 `rollbackFor = Exception.class`，把检查异常也纳入回滚范围。

## 2.4 Spring、MVC、AOP

### IOC/DI 是什么？

IOC 是把对象创建和依赖关系管理交给容器；DI 是容器将依赖注入对象。项目主要使用构造器注入，`@RequiredArgsConstructor` 由 Lombok 生成构造器，依赖清晰、便于测试，也避免字段注入带来的隐式依赖。

### Filter 和 HandlerInterceptor 有什么区别？

Filter 属于 Servlet 请求链，通常在 Spring MVC 进入 DispatcherServlet 前执行，可以包装请求体和响应体；Interceptor 属于 Spring MVC，围绕 Handler 执行，适合登录态恢复、登录校验等和 Controller 映射相关的逻辑。项目中 `TraceIdFilter`、`SignatureFilter` 负责开放请求链；`RefreshTokenInterceptor`、`LoginInterceptor` 负责控制台请求。

### 本项目怎么实现登录态？

注册密码用 BCrypt 哈希；登录成功后生成随机 UUID Token，把用户 ID、用户名、昵称、角色存入 Redis Hash 并设置 TTL。`RefreshTokenInterceptor` 读取 Token 恢复 `UserHolder`，同时滑动续期；`LoginInterceptor` 拒绝没有登录态的受保护请求，并对 `/admin/` 检查管理员角色。请求完成时清理 ThreadLocal，避免 Servlet 线程复用导致用户信息串用。登出删除 Redis key，使 Token 立即失效。

### AOP 适合做什么？项目里怎么用？

AOP 适合把审计、日志、权限周边等横切逻辑从业务主流程中抽离。项目 `@AuditLog` 注解由 `AuditLogAspect` 环绕指定方法，记录调用成功或失败；审计写入失败只记日志，不改变原业务结果。

Spring AOP 通常基于代理，因此同一个对象内部的 `this.method()` 自调用不经过代理，可能导致事务或切面不生效。项目的 `saveDirect` 定义在 `CallLogService`，由 `CallLogDispatchService` 和 `CallLogListener` 等其他 Bean 通过代理调用，`@Transactional` 才会生效；如果 `CallLogServiceImpl` 内部自调用 `saveDirect`，事务不会开启。

### Spring 事务什么时候生效？

事务通常由 Spring AOP 代理拦截带 `@Transactional` 的外部调用，在方法边界开启、提交或回滚。默认回滚规则主要针对运行时异常；项目显式使用 `rollbackFor = Exception.class`。事务是否生效还受代理调用、事务管理器、数据源和异常是否被吞掉等影响。

项目里的 `saveDirect` 把调用日志插入和日用量统计更新放在同一个数据库事务里：统计更新失败时，日志插入也回滚，避免只成功一半。

### `@ControllerAdvice` 的作用是什么？

集中处理 Controller 抛出的异常，将异常映射成统一响应结构和合适 HTTP 状态码，避免每个接口重复 try/catch。开放请求的过滤器还要能捕获过滤链中的异常；项目也会在响应缓存包装器中提取 Controller 返回的业务错误码用于调用日志。

## 2.5 结合代码的快速复述

```text
用户密码：BCrypt 单向哈希
用户登录：Redis Token + Hash + TTL + ThreadLocal 请求上下文
开放调用：Servlet Filter 先处理请求体与签名
普通管理接口：MVC Interceptor 恢复登录态并做角色校验
审计：@AuditLog + Spring AOP
数据库事务：由 Spring 代理在独立 Service Bean 的方法边界管理
```
