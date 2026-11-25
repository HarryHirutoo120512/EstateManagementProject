package com.javaweb.model.request;

import com.javaweb.model.dto.AbstractDTO;

import java.util.List;
import java.util.Objects;

public class BuildingSearchRequest extends AbstractDTO {
    private String name;
    private Long floorArea;
    private String district;
    private String ward;
    private String street;
    private Long numberOfBasement;
    private String direction;
    private Long level;
    private Long areaFrom;
    private Long areaTo;
    private Long rentPriceFrom;
    private Long rentPriceTo;
    private String managername;
    private String managerphone;
    private Long staffId;
    private List<String> typeCode;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getFloorArea() {
        return floorArea;
    }

    public void setFloorArea(Long floorArea) {
        this.floorArea = floorArea;
    }

    public Long getNumberOfBasement() {
        return numberOfBasement;
    }

    public void setNumberOfBasement(Long numberOfBasement) {
        this.numberOfBasement = numberOfBasement;
    }

    public List<String> getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(List<String> typeCode) {
        this.typeCode = typeCode;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getManagername() {
        return managername;
    }

    public void setManagername(String managername) {
        this.managername = managername;
    }

    public String getManagerphone() {
        return managerphone;
    }

    public void setManagerphone(String managerphone) {
        this.managerphone = managerphone;
    }

    public Long getRentPriceFrom() {
        return rentPriceFrom;
    }

    public void setRentPriceFrom(Long rentPriceFrom) {
        this.rentPriceFrom = rentPriceFrom;
    }

    public Long getRentPriceTo() {
        return rentPriceTo;
    }

    public void setRentPriceTo(Long rentPriceTo) {
        this.rentPriceTo = rentPriceTo;
    }

    public Long getAreaFrom() {
        return areaFrom;
    }

    public void setAreaFrom(Long areaFrom) {
        this.areaFrom = areaFrom;
    }

    public Long getAreaTo() {
        return areaTo;
    }

    public void setAreaTo(Long areaTo) {
        this.areaTo = areaTo;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Long getLevel() {
        return level;
    }

    public void setLevel(Long level) {
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuildingSearchRequest that = (BuildingSearchRequest) o;
        return Objects.equals(name, that.name)
                && Objects.equals(floorArea, that.floorArea)
                && Objects.equals(district, that.district)
                && Objects.equals(ward, that.ward)
                && Objects.equals(street, that.street)
                && Objects.equals(numberOfBasement, that.numberOfBasement)
                && Objects.equals(direction, that.direction)
                && Objects.equals(level, that.level)
                && Objects.equals(areaFrom, that.areaFrom)
                && Objects.equals(areaTo, that.areaTo)
                && Objects.equals(rentPriceFrom, that.rentPriceFrom)
                && Objects.equals(rentPriceTo, that.rentPriceTo)
                && Objects.equals(managername, that.managername)
                && Objects.equals(managerphone, that.managerphone)
                && Objects.equals(staffId, that.staffId)
                && Objects.equals(typeCode, that.typeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, floorArea, district, ward, street, numberOfBasement, direction, level,
                areaFrom, areaTo, rentPriceFrom, rentPriceTo, managername, managerphone, staffId, typeCode);
    }

    /** BẮT BUỘC cho Redis: key cache được tạo từ chuỗi này. */
    @Override
    public String toString() {
        return "BuildingSearchRequest{name=" + name + ", floorArea=" + floorArea + ", district=" + district
                + ", ward=" + ward + ", street=" + street + ", numberOfBasement=" + numberOfBasement
                + ", direction=" + direction + ", level=" + level + ", areaFrom=" + areaFrom + ", areaTo=" + areaTo
                + ", rentPriceFrom=" + rentPriceFrom + ", rentPriceTo=" + rentPriceTo
                + ", managername=" + managername + ", managerphone=" + managerphone
                + ", staffId=" + staffId + ", typeCode=" + typeCode + "}";
    }
}
