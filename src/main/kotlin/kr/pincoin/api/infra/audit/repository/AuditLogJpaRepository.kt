package kr.pincoin.api.infra.audit.repository

import kr.pincoin.api.infra.audit.entity.AuditLogEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogJpaRepository : JpaRepository<AuditLogEntity, Long>