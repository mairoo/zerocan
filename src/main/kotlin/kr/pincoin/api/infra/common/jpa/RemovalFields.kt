package kr.pincoin.api.infra.common.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * 삭제 여부를 담당하는 임베디드 타입입니다.
 */
@Embeddable
class RemovalFields {
    @Column(name = "is_removed")
    var isRemoved: Boolean = false
}
