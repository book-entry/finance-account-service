package com.personal.finance.account.mapper;

import com.personal.finance.account.dto.request.CreateAccountRequest;
import com.personal.finance.account.dto.response.AccountResponse;
import com.personal.finance.account.entity.Account;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Entity ⇄ DTO mapping for account-service. Spring component model so it can
 * be injected directly into the service layer.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper {

    /**
     * Builds a new {@link Account} from a create request. {@code userId},
     * {@code status}, {@code openingBalance} default, and timestamps are set
     * by the caller / persistence layer — never by the request body.
     */
    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Account toEntity(CreateAccountRequest request);

    AccountResponse toResponse(Account account);

    /**
     * Partial update — only non-null fields on the request are copied onto
     * the existing entity. {@code accountType} and {@code currency} are
     * intentionally absent from {@link com.personal.finance.account.dto.request.UpdateAccountRequest}
     * because spec §3.1 forbids changing them after creation.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "accountType", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "openingBalance", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void applyUpdate(com.personal.finance.account.dto.request.UpdateAccountRequest request,
                     @MappingTarget Account account);
}
