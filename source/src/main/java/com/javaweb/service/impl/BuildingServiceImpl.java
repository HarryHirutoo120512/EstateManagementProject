package com.javaweb.service.impl;

import com.javaweb.config.ModelMapperConfig;
import com.javaweb.config.RedisConfig;
import com.javaweb.converter.BuildingDTOConverter;
import com.javaweb.entity.BuildingEntity;
import com.javaweb.entity.RentAreaEntity;
import com.javaweb.entity.UserEntity;
import com.javaweb.exception.ServiceException;
import com.javaweb.model.event.ActivityEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.javaweb.model.dto.BuildingDTO;
import com.javaweb.model.dto.MyUserDetail;
import com.javaweb.model.request.BuildingSearchRequest;
import com.javaweb.model.response.BuildingSearchResponse;
import com.javaweb.model.response.ResponseDTO;
import com.javaweb.model.response.StaffResponseDTO;
import com.javaweb.repository.AssignmentBuildingRepository;
import com.javaweb.repository.BuildingRepository;
import com.javaweb.repository.RentAreaRepository;
import com.javaweb.repository.UserRepository;
import com.javaweb.security.utils.SecurityUtils;
import com.javaweb.service.IBuildingService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BuildingServiceImpl implements IBuildingService {
    @Autowired
    public BuildingRepository buildingRepository;
    @Autowired
    public UserRepository userRepository;
    @Autowired
    public RentAreaRepository rentAreaRepository;
    @Autowired
    public AssignmentBuildingRepository assignmentBuildingRepository;
    @Autowired
    public BuildingDTOConverter buildingDTOConverter;
    @Autowired
    public ModelMapperConfig modelMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public ResponseDTO listStaffs(Long buildingId) {
        if (buildingId == null) {
            throw new ServiceException("ID tòa nhà không được để trống");
        }
        BuildingEntity building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy tòa nhà với id = " + buildingId));
        List<UserEntity> staffs = userRepository.findByStatusAndRoles_Code(1, "STAFF");
        List<UserEntity> staffAssignment = userRepository.findUsersByBuilding(building);
        List<StaffResponseDTO> staffResponseDTOS = new ArrayList<>();
        ResponseDTO responseDTO = new ResponseDTO();
        for (UserEntity it : staffs) {
            StaffResponseDTO staffResponseDTO = new StaffResponseDTO();
            staffResponseDTO.setFullName(it.getFullName());
            staffResponseDTO.setStaffId(it.getId());
            if (staffAssignment != null && staffAssignment.contains(it)) {
                staffResponseDTO.setChecked("checked");
            } else {
                staffResponseDTO.setChecked("");
            }
            staffResponseDTOS.add(staffResponseDTO);
        }
        responseDTO.setData(staffResponseDTOS);
        responseDTO.setMessage("success");
        return responseDTO;
    }

    @Override
    @Cacheable(value = RedisConfig.BUILDINGS_CACHE,
            key = "T(com.javaweb.security.utils.SecurityUtils).cacheScope() + ':' + #p0 + ':' + #p1")
    public List<BuildingSearchResponse> findAll(BuildingSearchRequest buildingSearchRequest, Pageable pageable) {
        if (buildingSearchRequest == null) {
            buildingSearchRequest = new BuildingSearchRequest();
        }
        if (SecurityUtils.isStaffOnly()) {
            MyUserDetail currentUser = SecurityUtils.getPrincipal();
            if (currentUser != null && currentUser.getId() != null) {
                buildingSearchRequest.setStaffId(currentUser.getId());
            }
        }
        List<BuildingEntity> buildingEntities = buildingRepository.findAll(buildingSearchRequest, pageable);
        List<BuildingSearchResponse> result = new ArrayList<>();
        for (BuildingEntity building : buildingEntities) {
            BuildingSearchResponse b = buildingDTOConverter.toBuildingSearchResponse(building);
            result.add(b);
        }
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.BUILDINGS_CACHE, allEntries = true)
    public void deleteBuildings(List<Long> ids) {
        if (SecurityUtils.isStaffOnly()) {
            throw new AccessDeniedException("Nhân viên không có quyền xóa tòa nhà");
        }
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("Danh sách ID tòa nhà cần xóa không được để trống");
        }
        for (Long id : ids) {
            if (!buildingRepository.findById(id).isPresent()) {
                throw new ServiceException("Không tìm thấy tòa nhà với id = " + id);
            }
            buildingRepository.deleteAssignmentByBuildingId(id);
            buildingRepository.deleteRentAreaByBuildingId(id);
            buildingRepository.deleteById(id);
        }
    }

    @Override
    public BuildingDTO findBuildingById(Long id) {
        if (id == null) {
            throw new ServiceException("ID tòa nhà không được để trống");
        }
        BuildingEntity building = buildingRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Không tìm thấy tòa nhà với id = " + id));
        return buildingDTOConverter.toBuildingDTO_forUpdateBuilding(building);
    }

    @Transactional
    @Override
    @CacheEvict(value = RedisConfig.BUILDINGS_CACHE, allEntries = true)
    public void addOrUpdateBuildings(BuildingDTO buildingDTO) {
        if (buildingDTO == null) {
            throw new ServiceException("Thông tin tòa nhà không được để trống");
        }
        if (StringUtils.isBlank(buildingDTO.getName())) {
            throw new ServiceException("Tên tòa nhà không được để trống");
        }
        if (buildingDTO.getTypeCode() == null || buildingDTO.getTypeCode().isEmpty()) {
            throw new ServiceException("Loại tòa nhà không được để trống");
        }

        BuildingEntity updateOrAddBuilding = buildingDTOConverter.toBuildingEntity(buildingDTO);
        BuildingEntity existingBuilding;
        if (buildingDTO.getId() != null) {
            existingBuilding = buildingRepository.findById(buildingDTO.getId())
                    .orElseThrow(() -> new ServiceException("Không tìm thấy tòa nhà với id = " + buildingDTO.getId()));
        } else {
            existingBuilding = new BuildingEntity();
        }
        existingBuilding = buildingDTOConverter.entityToEntity(existingBuilding, updateOrAddBuilding);
        BuildingEntity savedBuilding = buildingRepository.save(existingBuilding);
        if (savedBuilding != null) {
            existingBuilding = savedBuilding;
        }
        rentAreaRepository.deleteBuildingById(existingBuilding.getId());
        if (updateOrAddBuilding.getItems() != null) {
            for (RentAreaEntity rentAreaEntity : updateOrAddBuilding.getItems()) {
                if (rentAreaEntity != null && StringUtils.isNotBlank(rentAreaEntity.getValue())) {
                    rentAreaEntity.setBuilding(existingBuilding);
                    rentAreaRepository.save(rentAreaEntity);
                }
            }
        }
        String action = buildingDTO.getId() == null ? "Thêm" : "Sửa";
        eventPublisher.publishEvent(new ActivityEvent(ActivityEvent.BUILDING_SAVED,
                action + " tòa nhà id=" + existingBuilding.getId() + ", tên=" + buildingDTO.getName()));
    }

    @Override
    public int countTotalItem() {
        return buildingRepository.countTotalItem();
    }
}
