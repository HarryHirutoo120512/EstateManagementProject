package com.javaweb.service.impl;

import com.javaweb.converter.CustomerConverter;
import com.javaweb.entity.CustomerEntity;
import com.javaweb.entity.UserEntity;
import com.javaweb.exception.ServiceException;
import com.javaweb.model.event.ActivityEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.javaweb.model.dto.CustomerDTO;
import com.javaweb.model.dto.MyUserDetail;
import com.javaweb.model.request.CustomerSearchRequest;
import com.javaweb.model.response.CustomerSearchResponse;
import com.javaweb.model.response.ResponseDTO;
import com.javaweb.model.response.StaffResponseDTO;
import com.javaweb.repository.CustomerRepository;
import com.javaweb.repository.UserRepository;
import com.javaweb.security.utils.SecurityUtils;
import com.javaweb.service.ICustomerService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerServiceImpl implements ICustomerService {
    @Autowired
    public CustomerRepository customerRepository;

    @Autowired
    public UserRepository userRepository;

    @Autowired
    public CustomerConverter customerConverter;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public ResponseDTO listStaffs(Long customerId) {
        if (customerId == null) {
            throw new ServiceException("ID khách hàng không được để trống");
        }
        CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy khách hàng với id = " + customerId));
        List<UserEntity> staffs = userRepository.findByStatusAndRoles_Code(1, "STAFF");
        List<UserEntity> staffAssignment = customerEntity.getUserEntities();
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
    public List<CustomerSearchResponse> findAll(CustomerSearchRequest customerSearchRequest, Pageable pageable) {
        if (customerSearchRequest == null) {
            customerSearchRequest = new CustomerSearchRequest();
        }
        if (SecurityUtils.isStaffOnly()) {
            MyUserDetail currentUser = SecurityUtils.getPrincipal();
            if (currentUser != null && currentUser.getId() != null) {
                customerSearchRequest.setStaffId(currentUser.getId());
            }
        }
        List<CustomerEntity> customerEntities = customerRepository.findAll(customerSearchRequest, pageable);
        List<CustomerSearchResponse> result = new ArrayList<>();
        for (CustomerEntity it : customerEntities) {
            CustomerSearchResponse customerSearchResponse = customerConverter.toCustomerSearchResponse(it);
            result.add(customerSearchResponse);
        }
        return result;
    }

    @Override
    @Transactional
    public void deleteCustomers(List<Long> ids) {
        if (SecurityUtils.isStaffOnly()) {
            throw new AccessDeniedException("Nhân viên không có quyền xóa khách hàng");
        }
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("Danh sách ID khách hàng cần xóa không được để trống");
        }
        for (Long id : ids) {
            CustomerEntity customer = customerRepository.findById(id)
                    .orElseThrow(() -> new ServiceException("Không tìm thấy khách hàng với id = " + id));
            customer.setIsActive(0);
            customerRepository.save(customer);
        }
    }

    @Override
    public CustomerDTO findCustomerById(Long id) {
        if (id == null) {
            throw new ServiceException("ID khách hàng không được để trống");
        }
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Không tìm thấy khách hàng với id = " + id));
        return customerConverter.toCustomerDTO(customer);
    }

    @Override
    public int countTotalItem() {
        return customerRepository.countTotalItem();
    }

    @Override
    @Transactional
    public void addOrUpdateCustomers(CustomerDTO customerDTO) {
        if (customerDTO == null) {
            throw new ServiceException("Thông tin khách hàng không được để trống");
        }
        if (StringUtils.isBlank(customerDTO.getFullname())) {
            throw new ServiceException("Tên khách hàng không được để trống");
        }
        if (StringUtils.isBlank(customerDTO.getCustomerPhone())) {
            throw new ServiceException("Số điện thoại khách hàng không được để trống");
        }

        CustomerEntity updateOrAddCustomer = customerConverter.toCustomerEntity(customerDTO);
        if (updateOrAddCustomer.getId() != null) {
            CustomerEntity customerEntity = customerRepository.findById(updateOrAddCustomer.getId())
                    .orElseThrow(() -> new ServiceException("Không tìm thấy khách hàng với id = " + updateOrAddCustomer.getId()));
            updateOrAddCustomer.setUserEntities(customerEntity.getUserEntities());
        }
        updateOrAddCustomer.setIsActive(1);
        customerRepository.save(updateOrAddCustomer);
        eventPublisher.publishEvent(new ActivityEvent(ActivityEvent.CUSTOMER_SAVED,
                "Lưu khách hàng id=" + updateOrAddCustomer.getId() + ", tên=" + customerDTO.getFullname()));
    }
}
