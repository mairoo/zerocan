package kr.pincoin.api.infra.user.mapper

import kr.pincoin.api.domain.user.model.User
import kr.pincoin.api.infra.user.entity.UserEntity
import kr.pincoin.api.infra.user.entity.UserRoleEntity

fun UserEntity?.toModel(
    userRoles: List<UserRoleEntity>? = null,
): User? =
    this?.let { entity ->
        val roles = userRoles
            ?.filter { it.userId == entity.id && !it.removalFields.isRemoved }
            ?.map { it.role }
            ?: emptyList()

        User.of(
            id = entity.id,
            created = entity.dateTimeFields.createdAt,
            modified = entity.dateTimeFields.modifiedAt,
            isRemoved = entity.removalFields.isRemoved,
            isActive = entity.isActive,
            name = entity.name,
            email = entity.email,
            roles = roles,
        )
    }

fun List<UserEntity>?.toModelList(
    userRolesMap: Map<Long, List<UserRoleEntity>> = emptyMap(),
): List<User> =
    this?.mapNotNull { entity ->
        entity.toModel(userRolesMap[entity.id] ?: emptyList())
    } ?: emptyList()

fun User?.toEntity(): UserEntity? =
    this?.let { model ->
        UserEntity.of(
            id = model.id,
            isRemoved = model.isRemoved,
            isActive = model.isActive,
            name = model.name,
            email = model.email,
            // created, modified: 매핑 안 함 JPA Auditing 관리 필드
        )
    }