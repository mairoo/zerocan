package kr.pincoin.api.domain.audit.repository

import kr.pincoin.api.domain.audit.model.AuditLog
import org.springframework.data.domain.Page

interface AuditLogRepository {
    fun save(auditLog: AuditLog): AuditLog

    fun findAuditLogs(): Page<AuditLog>
}