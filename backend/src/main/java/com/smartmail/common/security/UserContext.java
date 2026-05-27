package com.smartmail.common.security;

import com.smartmail.common.exception.BusinessException;

public final class UserContext {
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long requireUserId() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        return user.id();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
