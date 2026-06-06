package com.qros.modules.user.mapper;

import com.qros.modules.user.dto.UserResponse;
import com.qros.modules.user.dto.UserUpsertRequest;
import com.qros.modules.user.model.User;
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
     * Converts a UserUpsertRequest DTO to a User entity.
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
    User toEntity(UserUpsertRequest request);

    /**
     * Updates an existing User entity with data from a UserUpsertRequest.
     * 
     * @param entity The entity to be updated
     * @param request The source DTO containing updated values
     */
    @InheritConfiguration(name = "toEntity")
    void updateEntity(@MappingTarget User entity, UserUpsertRequest request);

    /**
     * Safe helper logic to convert a status string to its corresponding Enum.
     * 
     * @param status The status string
     * @return The corresponding UserStatus enum or ACTIVE as default
     */
    @Named("stringToStatus")
    default User.UserStatus mapStatus(String status) {
        if (status == null)
            return null;
        try {
            return User.UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return User.UserStatus.ACTIVE;
        }
    }
}
