package kr.pincoin.api.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "partition")
data class PartitionProperties(
    val tables: Map<String, TablePartitionConfig> = emptyMap(),
    val futurePeriods: FuturePeriodsConfig = FuturePeriodsConfig()
) {
    data class TablePartitionConfig(
        val tableName: String,
        val partitionType: PartitionType,
        val retentionMonths: Int
    )

    data class FuturePeriodsConfig(
        val daily: Int = 7,
        val monthly: Int = 3
    )

    enum class PartitionType {
        DAILY,
        MONTHLY
    }
}