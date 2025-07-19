package kr.pincoin.api.infra.common.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EntityListeners
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * 생성자/수정자 정보를 담당하는 임베디드 타입입니다.
 */
@Embeddable
@EntityListeners(AuditingEntityListener::class)
class AuditorFields {
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    var createdBy: Long? = null

    @LastModifiedBy
    @Column(name = "modified_by")
    var modifiedBy: Long? = null
}