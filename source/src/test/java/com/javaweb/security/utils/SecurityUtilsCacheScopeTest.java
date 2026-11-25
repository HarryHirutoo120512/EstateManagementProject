package com.javaweb.security.utils;

import com.javaweb.model.dto.MyUserDetail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsCacheScopeTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long id, String... roles) {
        MyUserDetail user = new MyUserDetail("u", "p", true, true, true, true,
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
        user.setId(id);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    @DisplayName("STAFF -> vùng cache riêng STAFF_<id>")
    void staffGetsOwnScope() {
        login(5L, "ROLE_STAFF");
        assertEquals("STAFF_5", SecurityUtils.cacheScope());
    }

    @Test
    @DisplayName("MANAGER -> vùng ALL")
    void managerGetsAllScope() {
        login(1L, "ROLE_MANAGER");
        assertEquals("ALL", SecurityUtils.cacheScope());
    }

    @Test
    @DisplayName("Hai nhân viên khác nhau không bao giờ dùng chung vùng cache")
    void twoStaffNeverShareScope() {
        login(5L, "ROLE_STAFF");
        String a = SecurityUtils.cacheScope();
        login(6L, "ROLE_STAFF");
        String b = SecurityUtils.cacheScope();
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("STAFF không bao giờ nhận scope ALL của MANAGER")
    void staffScopeDiffersFromManagerScope() {
        login(5L, "ROLE_STAFF");
        String staff = SecurityUtils.cacheScope();
        login(1L, "ROLE_MANAGER");
        assertNotEquals(staff, SecurityUtils.cacheScope());
    }
}
