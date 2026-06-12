package com.qros.modules.user.mapper;

import com.qros.modules.user.dto.response.UserResponse;
import com.qros.modules.user.dto.request.CreateUserRequest;
import com.qros.modules.user.dto.request.UpdateUserRequest;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserStatus;
import org.mapstruct.*;

/**
 * UserMapper - MapStruct interface for converting between User entities and DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    /**
     * Converts a User entity to a UserResponse DTO.
     * 
     * @param user The source entity
     * @return The target DTO
     */
    UserResponse toDto(User user);

    /**
     * Converts a CreateUserRequest DTO to a User entity.
     * Core sensitive fields are ignored during basic mapping and handled in services.
     * 
     * @param request The source DTO
     * @return The target entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toEntity(CreateUserRequest request);

    /**
     * Updates an existing User entity with data from a CreateUserRequest.
     * 
     * @param entity The entity to be updated
     * @param request The source DTO containing updated values
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "role", ignore = true)
    void updateEntity(@MappingTarget User entity, UpdateUserRequest request);

}
