package com.javaweb.service.impl;

import com.javaweb.model.dto.MyUserDetail;
import com.javaweb.model.dto.RoleDTO;
import com.javaweb.model.dto.UserDTO;
import com.javaweb.service.IUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailServiceTest {

    @Mock
    private IUserService userService;

    @InjectMocks
    private CustomUserDetailService customUserDetailService;

    private UserDTO user(Long id, String username, String... roleCodes) {
        UserDTO user = new UserDTO();
        user.setId(id);
        user.setUserName(username);
        user.setFullName("Nguyễn Văn A");
        user.setPassword("$2a$10$hashedPassword");
        user.setStatus(1);
        List<RoleDTO> roles = new ArrayList<>();
        for (String code : roleCodes) {
            RoleDTO role = new RoleDTO();
            role.setCode(code);
            roles.add(role);
        }
        user.setRoles(roles);
        return user;
    }

    private Set<String> authorityNames(UserDetails details) {
        return details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Đăng nhập đúng tài khoản MANAGER -> UserDetails có username, password và ROLE_MANAGER")
    void loadUserByUsername_activeManager_returnsRoleManager() {
        when(userService.findOneByUserNameAndStatus("nguyenvana", 1))
                .thenReturn(user(1L, "nguyenvana", "MANAGER"));

        UserDetails details = customUserDetailService.loadUserByUsername("nguyenvana");

        assertEquals("nguyenvana", details.getUsername());
        assertEquals("$2a$10$hashedPassword", details.getPassword());
        assertEquals(Collections.singleton("ROLE_MANAGER"), authorityNames(details));
    }

    @Test
    @DisplayName("User có 2 role -> đủ 2 GrantedAuthority, id và fullName được copy sang MyUserDetail")
    void loadUserByUsername_twoRoles_returnsBothAuthoritiesAndCopiesInfo() {
        when(userService.findOneByUserNameAndStatus("hiep", 1))
                .thenReturn(user(7L, "hiep", "MANAGER", "STAFF"));

        UserDetails details = customUserDetailService.loadUserByUsername("hiep");

        assertEquals(new HashSet<>(Arrays.asList("ROLE_MANAGER", "ROLE_STAFF")), authorityNames(details));
        MyUserDetail myUser = assertInstanceOf(MyUserDetail.class, details);
        assertEquals(7L, myUser.getId());
        assertEquals("Nguyễn Văn A", myUser.getFullName());
    }

    @Test
    @DisplayName("Username không tồn tại -> ném UsernameNotFoundException")
    void loadUserByUsername_notFound_throwsUsernameNotFound() {
        when(userService.findOneByUserNameAndStatus("khongco", 1)).thenReturn(null);

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailService.loadUserByUsername("khongco"));

        assertEquals("Username not found", ex.getMessage());
    }

    @Test
    @DisplayName("User bị khóa (status = 0) -> service chỉ tìm status = 1 nên không đăng nhập được")
    void loadUserByUsername_lockedUser_throwsBecauseOnlyActiveUserIsQueried() {
        when(userService.findOneByUserNameAndStatus("bikhoa", 1)).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailService.loadUserByUsername("bikhoa"));

        verify(userService).findOneByUserNameAndStatus("bikhoa", 1);
        verify(userService, never()).findOneByUserNameAndStatus("bikhoa", 0);
    }
}