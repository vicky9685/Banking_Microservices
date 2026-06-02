package com.bank.customer.mapper;

import com.bank.customer.domain.Address;
import com.bank.customer.domain.Customer;
import com.bank.customer.domain.KycStatus;
import com.bank.customer.dto.CustomerDtos.AddressDto;
import com.bank.customer.dto.CustomerDtos.CreateCustomerRequest;
import com.bank.customer.dto.CustomerDtos.CustomerResponse;
import com.bank.customer.dto.CustomerDtos.UpdateCustomerRequest;
import org.mapstruct.*;

/**
 * MapStruct mapper. Compile-time generated, zero reflection overhead.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "kycStatus", expression = "java(KycStatus.PENDING)")
    Customer toEntity(CreateCustomerRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateCustomerRequest request, @MappingTarget Customer target);

    CustomerResponse toResponse(Customer customer);

    AddressDto toDto(Address address);
    Address toEntity(AddressDto dto);
}
