package com.example.dapprototype.mapper;

import com.example.dapprototype.model.CustomerRequest;
import com.example.dapprototype.model.RequestInfo;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    RequestMapper INSTANCE = Mappers.getMapper(RequestMapper.class);

    @Mapping(source = "activityId", target = "activityId")    
    CustomerRequest toCustomerRequest(RequestInfo requestInfo);
    
    @AfterMapping
    default void populateCustomerIds(RequestInfo requestInfo, @MappingTarget CustomerRequest customerRequest) {
        List<String> customerIds = new ArrayList<>();
        Map<String, String> customerTags = new HashMap<>();
        
        if (requestInfo.getPayeeCustomerId() != null) {
            customerIds.add(requestInfo.getPayeeCustomerId());
            customerTags.put(requestInfo.getPayeeCustomerId(), "payeeCustomer");
        }
        if (requestInfo.getPayerCustomerId() != null) {
            customerIds.add(requestInfo.getPayerCustomerId());
            customerTags.put(requestInfo.getPayerCustomerId(), "payerCustomer");
        }
        
        customerRequest.setCustomerIds(customerIds);
        customerRequest.setCustomerTags(customerTags);
    }
}
