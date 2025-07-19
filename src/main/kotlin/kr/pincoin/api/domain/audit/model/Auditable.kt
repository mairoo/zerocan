package kr.pincoin.api.domain.audit.model

interface Auditable {
    var originState: Map<String, String?>

    fun getState(): Map<String, String?>

    fun getEntityId(): String

    fun getEntityType(): String
}