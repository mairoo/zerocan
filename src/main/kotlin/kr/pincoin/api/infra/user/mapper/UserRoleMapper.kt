package kr.pincoin.api.infra.user.mapper

import kr.pincoin.api.domain.user.model.UserRole
import kr.pincoin.api.infra.user.entity.UserRoleEntity

fun UserRoleEntity?.toModel(): UserRole? =
    this?.let { entity ->
        UserRole.of(
            id = entity.id,
            created = entity.dateTimeFields.createdAt,
            modified = entity.dateTimeFields.modifiedAt,
            isRemoved = entity.removalFields.isRemoved,
            userId = entity.userId,
            role = entity.role,
        )
    }

fun List<UserRoleEntity>?.toModelList(): List<UserRole> =
    this?.mapNotNull { it.toModel() } ?: emptyList()

fun UserRole?.toEntity(): UserRoleEntity? =
    this?.let { model ->
        UserRoleEntity.of(
            id = model.id,
            isRemoved = model.isRemoved,
            userId = model.userId,
            role = model.role,
            // created, modified: 매핑 안 함 JPA Auditing 관리 필드
        )
    }