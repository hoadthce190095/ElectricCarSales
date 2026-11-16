package com.electriccarsales.controller;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý trạng thái phiên đăng nhập của người dùng để hủy phiên khi tài khoản bị khóa/ngưng hoạt động.
 */
public final class AccountSessions {
    private static final Set<Integer> INVALIDATED_USERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private AccountSessions() {
    }

    public static void invalidate(int userId) {
        INVALIDATED_USERS.add(userId);
    }

    public static boolean isInvalidated(int userId) {
        return INVALIDATED_USERS.contains(userId);
    }

    public static void clear(int userId) {
        INVALIDATED_USERS.remove(userId);
    }
}
