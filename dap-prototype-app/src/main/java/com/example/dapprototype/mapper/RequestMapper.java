package com.example.dapprototype.mapper;

import com.example.dapprototype.model.CustomerEnrichment;
import com.example.dapprototype.model.RequestPayload;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    RequestMapper INSTANCE = Mappers.getMapper(RequestMapper.class);

    @Mapping(source = "requestInfo.activityId", target = "activityId")
    CustomerEnrichment toCustomerEnrichment(RequestPayload requestPayload);
}
