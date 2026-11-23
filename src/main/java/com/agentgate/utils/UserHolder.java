package com.agentgate.utils;

import com.agentgate.dto.UserDTO;
/**
 * 教学说明：静态 ThreadLocal 保存当前线程 UserDTO，提供取用户/ID 和 remove；请求结束必须清理，不能当作跨线程身份存储。
 * 文件路径：src/main/java/com/agentgate/utils/UserHolder.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public final class UserHolder {

    private static final ThreadLocal<UserDTO> USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 私有构造器禁止直接实例化，仅通过静态方法使用此工具类。
     */
    private UserHolder() {
    }

    /**
     * 把用户快照保存到当前线程；不能跨线程传递登录身份。
     */
    public static void saveUser(UserDTO user) {
        USER_THREAD_LOCAL.set(user);
    }

    /**
     * 取得当前线程用户快照，未建立上下文时返回空值。
     */
    public static UserDTO getUser() {
        return USER_THREAD_LOCAL.get();
    }

    /**
     * 读取当前线程用户 ID，未登录时返回空值。
     */
    public static Long getUserId() {
        UserDTO user = USER_THREAD_LOCAL.get();
        return user == null ? null : user.getId();
    }

    /**
     * 删除 ThreadLocal 引用，必须在请求结束执行。
     */
    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}
