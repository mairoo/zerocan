package kr.pincoin.api.infra.audit.repository

import kr.pincoin.api.domain.audit.model.AuditLog
import kr.pincoin.api.domain.audit.repository.AuditLogRepository
import kr.pincoin.api.infra.audit.mapper.toEntity
import kr.pincoin.api.infra.audit.mapper.toModel
import org.springframework.data.domain.Page
import org.springframework.stereotype.Repository

@Repository
class AuditLogRepositoryImpl(
    private val jpaRepository: AuditLogJpaRepository,
) : AuditLogRepository {
    override fun save(auditLog: AuditLog): AuditLog =
        auditLog.toEntity()?.let { jpaRepository.save(it) }?.toModel()
            ?: throw IllegalArgumentException("감사 로그 저장 실패")

    override fun findAuditLogs(): Page<AuditLog> {
        TODO("Not yet implemented")
    }
}