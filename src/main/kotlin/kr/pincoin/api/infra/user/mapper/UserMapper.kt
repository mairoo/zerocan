package kr.pincoin.api.infra.user.mapper

import kr.pincoin.api.domain.user.model.User
import kr.pincoin.api.infra.user.entity.UserEntity


fun UserEntity?.toModel(
): User? =
    this?.let { entity ->
        User.of(
            id = entity.id,
            created = entity.dateTimeFields.createdAt,
            modified = entity.dateTimeFields.modifiedAt,
            isRemoved = entity.removalFields.isRemoved,
            isActive = entity.isActive,
            name = entity.name,
            email = entity.email,
        )
    }

fun List<UserEntity>?.toModelList(
): List<User> =
    this?.mapNotNull { it.toModel() } ?: emptyList()

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