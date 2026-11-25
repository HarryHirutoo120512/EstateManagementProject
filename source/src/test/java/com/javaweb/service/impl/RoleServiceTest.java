package com.javaweb.service.impl;

import com.javaweb.converter.RoleConverter;
import com.javaweb.entity.RoleEntity;
import com.javaweb.model.dto.RoleDTO;
import com.javaweb.repository.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoleConverter roleConverter;

    @InjectMocks
    private RoleService roleService;

    private RoleEntity roleEntity(String code, String name) {
        RoleEntity entity = new RoleEntity();
        entity.setCode(code);
        entity.setName(name);
        return entity;
    }

    private RoleDTO roleDTO(String code, String name) {
        RoleDTO dto = new RoleDTO();
        dto.setCode(code);
        dto.setName(name);
        return dto;
    }

    @Test
    @DisplayName("Có 2 role -> getRoles trả map code -> name đúng")
    void getRoles_twoRoles_returnsCodeToNameMap() {
        RoleEntity manager = roleEntity("MANAGER", "Quản lý");
        RoleEntity staff = roleEntity("STAFF", "Nhân viên");
        when(roleRepository.findAll()).thenReturn(Arrays.asList(manager, staff));
        when(roleConverter.convertToDto(manager)).thenReturn(roleDTO("MANAGER", "Quản lý"));
        when(roleConverter.convertToDto(staff)).thenReturn(roleDTO("STAFF", "Nhân viên"));

        Map<String, String> roles = roleService.getRoles();

        assertEquals(2, roles.size());
        assertEquals("Quản lý", roles.get("MANAGER"));
        assertEquals("Nhân viên", roles.get("STAFF"));
    }

    @Test
    @DisplayName("Bảng role rỗng -> trả map rỗng (không null), không gọi converter")
    void getRoles_noRole_returnsEmptyMap() {
        when(roleRepository.findAll()).thenReturn(Collections.emptyList());

        Map<String, String> roles = roleService.getRoles();

        assertNotNull(roles);
        assertTrue(roles.isEmpty());
        verifyNoInteractions(roleConverter);
    }

    @Test
    @DisplayName("findAll -> convert từng RoleEntity sang RoleDTO, giữ đúng thứ tự")
    void findAll_twoRoles_returnsDtoListInSameOrder() {
        RoleEntity manager = roleEntity("MANAGER", "Quản lý");
        RoleEntity staff = roleEntity("STAFF", "Nhân viên");
        when(roleRepository.findAll()).thenReturn(Arrays.asList(manager, staff));
        when(roleConverter.convertToDto(manager)).thenReturn(roleDTO("MANAGER", "Quản lý"));
        when(roleConverter.convertToDto(staff)).thenReturn(roleDTO("STAFF", "Nhân viên"));

        List<RoleDTO> result = roleService.findAll();

        assertEquals(2, result.size());
        assertEquals("MANAGER", result.get(0).getCode());
        assertEquals("STAFF", result.get(1).getCode());
    }
}