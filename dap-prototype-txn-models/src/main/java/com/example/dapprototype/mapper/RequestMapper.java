package com.example.dapprototype.mapper;

import com.example.dapprototype.model.CustomerRequest;
import com.example.dapprototype.model.RequestInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    RequestMapper INSTANCE = Mappers.getMapper(RequestMapper.class);

    @Mapping(source = "activityId", target = "activityId")
    CustomerRequest toCustomerRequest(RequestInfo requestInfo);
}
