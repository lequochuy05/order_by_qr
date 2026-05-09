package com.sacmauquan.qrordering.mapper;

import com.sacmauquan.qrordering.dto.UserResponse;
import com.sacmauquan.qrordering.dto.UserUpsertRequest;
import com.sacmauquan.qrordering.model.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    // MapStruct tự động hiểu Enum.name() -> String
    UserResponse toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toEntity(UserUpsertRequest request);

    @InheritConfiguration(name = "toEntity") // Kế thừa các rule ignore từ toEntity
    @Mapping(target = "email", ignore = true) // Email không cho phép sửa
    void updateEntity(@MappingTarget User entity, UserUpsertRequest request);

    /**
     * Logic bổ trợ để chuyển đổi String sang Enum an toàn
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
