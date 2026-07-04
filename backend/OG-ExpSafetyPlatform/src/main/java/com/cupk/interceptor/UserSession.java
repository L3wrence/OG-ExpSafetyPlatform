package com.cupk.interceptor;

import java.util.List;

public record UserSession(Long userId, List<String> roles, List<String> permissions) {
}
