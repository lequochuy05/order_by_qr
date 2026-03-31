package com.sacmauquan.qrordering.mapper;

import com.sacmauquan.qrordering.dto.UserDto;
import com.sacmauquan.qrordering.dto.UserUpsertRequest;
import com.sacmauquan.qrordering.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface UserMapper {

    @Mapping(target = "status", expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    UserDto toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true) // Handled securely in Service
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "status", ignore = true) // Handled in Service
    @Mapping(target = "role", ignore = true) // Handled in Service
    User toEntity(UserUpsertRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true) // Don't allow changing email on update
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "status", source = "status")
    @Mapping(target = "role", ignore = true) // Handled in Service
    void updateEntity(@MappingTarget User entity, UserUpsertRequest request);
}
