package com.javaweb.converter;

import com.javaweb.entity.BuildingEntity;
import com.javaweb.entity.RentAreaEntity;
import com.javaweb.model.dto.BuildingDTO;
import com.javaweb.model.response.BuildingSearchResponse;
import com.javaweb.utils.DistrictCode;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BuildingDTOConverter {
    @Autowired
    private ModelMapper modelMapper;

    public BuildingSearchResponse toBuildingSearchResponse(BuildingEntity item) {
        BuildingSearchResponse building = modelMapper.map(item, BuildingSearchResponse.class);
        building.setName(item.getName());
        if (StringUtils.isNotBlank(item.getDistrict())) {
            String districtkey = item.getDistrict();
            String districtvalue = DistrictCode.type().get(districtkey);
            building.setAddress(item.getStreet() + " " + item.getWard() + " " + districtvalue);
        } else {
            building.setAddress(item.getStreet() + " " + item.getWard());
        }
        if (item.getItems() != null) {
            String result = item.getItems().stream()
                    .filter(it -> it != null && it.getValue() != null)
                    .map(it -> it.getValue().toString())
                    .collect(Collectors.joining(","));
            building.setRentArea(result);
        } else {
            building.setRentArea("");
        }
        return building;
    }

    public BuildingEntity toBuildingEntity(BuildingDTO item) {
        BuildingEntity buildingEntity = modelMapper.map(item, BuildingEntity.class);
        if (item.getTypeCode() != null && !item.getTypeCode().isEmpty()) {
            String typeCodeJoin = item.getTypeCode().stream()
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.joining(","));
            buildingEntity.setType(typeCodeJoin);
        } else {
            buildingEntity.setType("");
        }

        List<RentAreaEntity> rentAreaEntities = new ArrayList<>();
        if (StringUtils.isNotBlank(item.getRentArea())) {
            String[] rentAreas = item.getRentArea().split(",");
            for (String rentArea : rentAreas) {
                if (StringUtils.isNotBlank(rentArea)) {
                    RentAreaEntity rentAreaEntity = new RentAreaEntity();
                    rentAreaEntity.setValue(rentArea.trim());
                    rentAreaEntities.add(rentAreaEntity);
                }
            }
        }
        buildingEntity.setItems(rentAreaEntities);
        return buildingEntity;
    }

    public BuildingEntity entityToEntity(BuildingEntity oldBuilding, BuildingEntity newBuildingEntity) {
        oldBuilding = modelMapper.map(newBuildingEntity, BuildingEntity.class);
        return oldBuilding;
    }

    public BuildingDTO toBuildingDTO_forUpdateBuilding(BuildingEntity buildingEntity) {
        BuildingDTO building = modelMapper.map(buildingEntity, BuildingDTO.class);
        List<RentAreaEntity> item = buildingEntity.getItems();
        if (item != null) {
            String rentAreas = item.stream()
                    .filter(it -> it != null && it.getValue() != null)
                    .map(it -> it.getValue().toString())
                    .collect(Collectors.joining(","));
            building.setRentArea(rentAreas);
        } else {
            building.setRentArea("");
        }
        if (StringUtils.isNotBlank(buildingEntity.getType())) {
            String[] typeCode = buildingEntity.getType().split(",");
            building.setTypeCode(new ArrayList<>(Arrays.asList(typeCode)));
        } else {
            building.setTypeCode(Collections.emptyList());
        }
        return building;
    }
}
