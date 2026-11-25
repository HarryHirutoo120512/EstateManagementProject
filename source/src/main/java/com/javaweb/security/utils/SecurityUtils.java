package com.javaweb.security.utils;

import com.javaweb.model.dto.MyUserDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SecurityUtils {

    public static MyUserDetail getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof MyUserDetail)) {
            return null;
        }
        return (MyUserDetail) authentication.getPrincipal();
    }

    public static List<String> getAuthorities() {
        List<String> results = new ArrayList<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return results;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            results.add(authority.getAuthority());
        }
        return results;
    }

    public static boolean isStaffOnly() {
        List<String> authorities = getAuthorities();
        return authorities.contains("ROLE_STAFF")
                && !authorities.contains("ROLE_MANAGER")
                && !authorities.contains("ROLE_ADMIN");
    }

    /**
     * Phạm vi dữ liệu của người đang đăng nhập, dùng làm 1 phần cache key.
     * STAFF chỉ thấy tòa nhà được giao -> mỗi nhân viên phải có vùng cache riêng.
     */
    public static String cacheScope() {
        if (isStaffOnly()) {
            MyUserDetail user = getPrincipal();
            return "STAFF_" + (user != null ? user.getId() : "unknown");
        }
        return "ALL";
    }
}
