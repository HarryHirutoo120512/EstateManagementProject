package com.javaweb.api.admin;

import com.javaweb.model.dto.BuildingDTO;
import com.javaweb.service.IBuildingService;
import com.javaweb.service.IUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BuildingAPI.class)   // chỉ khởi động tầng Web, KHÔNG có DB
class BuildingAPITest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IBuildingService buildingService;

    @MockBean
    private IUserService userService;   // BuildingAPI và CustomUserDetailService (trong WebSecurityConfig) đều cần

    @Test
    @WithMockUser(username = "manager1", roles = "MANAGER")
    @DisplayName("MANAGER xóa tòa nhà 1,2 -> HTTP 200, Service nhận đúng danh sách id")
    void deleteBuilding_asManager_returns200() throws Exception {
        mockMvc.perform(delete("/api/building/1,2"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(buildingService).deleteBuildings(Arrays.asList(1L, 2L));
    }

    @Test
    @WithMockUser(username = "staff1", roles = "STAFF")
    @DisplayName("STAFF xóa tòa nhà -> Service ném AccessDeniedException -> HTTP 403")
    void deleteBuilding_asStaff_returns403() throws Exception {
        doThrow(new AccessDeniedException("Nhân viên không có quyền xóa tòa nhà"))
                .when(buildingService).deleteBuildings(anyList());

        mockMvc.perform(delete("/api/building/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST thiếu body -> HTTP 400, Service không được gọi")
    void addOrUpdateBuilding_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/building").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(buildingService, never()).addOrUpdateBuildings(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST JSON hợp lệ -> HTTP 200, JSON được bind đúng vào BuildingDTO")
    void addOrUpdateBuilding_validJson_returns200() throws Exception {
        String json = "{\"name\":\"Nam Giao Building\",\"typeCode\":[\"NGUYEN_CAN\"],\"rentArea\":\"100,200\"}";

        mockMvc.perform(post("/api/building").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());

        ArgumentCaptor<BuildingDTO> captor = ArgumentCaptor.forClass(BuildingDTO.class);
        verify(buildingService).addOrUpdateBuildings(captor.capture());
        assertEquals("Nam Giao Building", captor.getValue().getName());
        assertEquals("100,200", captor.getValue().getRentArea());
    }
}