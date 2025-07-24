package kr.pincoin.api.infra.common.partition

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import kr.pincoin.api.global.properties.PartitionProperties
import kr.pincoin.api.infra.common.lock.RedisDistributedLock
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class PartitionManager(
    private val jdbcTemplate: JdbcTemplate,
    private val distributedLock: RedisDistributedLock,
    private val partitionProperties: PartitionProperties,
) {
    private val PARTITION_INIT_LOCK_KEY = "lock:partition:initialization"
    private val PARTITION_MANAGE_LOCK_KEY = "lock:partition:management"

    private val logger = KotlinLogging.logger {}

    // 파티션 이름 형식 매퍼
    private val partitionNameFormatters = mapOf(
        PartitionProperties.PartitionType.DAILY to { date: LocalDateTime ->
            "p_" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        },
        PartitionProperties.PartitionType.MONTHLY to { date: LocalDateTime ->
            "p_" + date.format(DateTimeFormatter.ofPattern("yyyyMM"))
        }
    )

    /**
     * 매일 정해진 시간에 실행되는 파티션 관리 스케줄러입니다.
     * 모든 설정된 테이블에 대해 미래 파티션을 생성하고 만료된 파티션을 제거합니다.
     * 오류 발생 시 관리자에게 알림을 발송합니다.
     */
    @Scheduled(cron = "0 0 1 * * *")
    fun managePartitions() {
        val (executed, _) = distributedLock.withCompletionTracking(
            PARTITION_MANAGE_LOCK_KEY,
            Duration.ofMinutes(5),
            Duration.ofMinutes(5),
        ) {
            logger.info { "파티션 관리 작업을 시작합니다" }

            // 모든 테이블에 대해 파티션 관리 작업 수행
            partitionProperties.tables.values.forEach { config ->
                // 미래 파티션 추가
                createFuturePartitions(config)

                // 오래된 파티션 제거
                deleteExpiredPartitions(config)
            }

            logger.info { "파티션 관리 작업이 성공적으로 완료되었습니다" }
        }

        if (!executed) {
            logger.info { "다른 인스턴스가 이미 파티션 관리 작업을 수행 중입니다." }
        }
    }

    /**
     * 애플리케이션 시작 시 테이블 파티션을 초기화합니다.
     */
    @PostConstruct
    fun initializePartitions() {
        logger.info { "파티션 초기화 작업 시작 시도" }

        val (executed, _) = distributedLock.withCompletionTracking(
            PARTITION_INIT_LOCK_KEY,
            Duration.ofMinutes(5),
            Duration.ofMinutes(5),
        ) {
            // 모든 테이블에 대해 파티션 초기화 확인
            partitionProperties.tables.values.forEach { config ->
                initializePartition(config)
            }

            logger.info { "파티션 초기화 작업이 완료되었습니다" }
        }

        if (!executed) {
            logger.info { "다른 인스턴스가 이미 파티션 초기화 작업을 수행했거나 수행 중입니다. 이 인스턴스는 초기화를 건너뜁니다." }
        }
    }

    /**
     * 지정된 테이블에 대한 파티션을 초기화합니다.
     */
    private fun initializePartition(config: PartitionProperties.TablePartitionConfig) {
        val hasPartitions = hasPartition(config.tableName)

        if (!hasPartitions) {
            logger.info { "테이블 [${config.tableName}]에 파티션이 없습니다. 초기 파티션을 설정합니다." }

            val sql = generateInitialPartitionSQL(config)
            try {
                jdbcTemplate.execute(sql)
                logger.info { "테이블 [${config.tableName}]에 초기 파티션이 설정되었습니다." }
            } catch (e: Exception) {
                logger.error(e) { "테이블 [${config.tableName}] 파티션 초기화 중 오류가 발생했습니다." }
                throw e
            }
        } else {
            logger.info { "테이블 [${config.tableName}]에 이미 파티션이 설정되어 있습니다." }
        }
    }

    /**
     * 지정된 테이블에 미래 파티션을 생성합니다.
     */
    private fun createFuturePartitions(config: PartitionProperties.TablePartitionConfig) {
        logger.info { "테이블 [${config.tableName}]의 미래 파티션 관리를 시작합니다" }

        val now = LocalDateTime.now()
        val numPeriods = when (config.partitionType) {
            PartitionProperties.PartitionType.DAILY -> partitionProperties.futurePeriods.daily
            PartitionProperties.PartitionType.MONTHLY -> partitionProperties.futurePeriods.monthly
        }

        // 파티션 타입에 따라 미래 날짜 생성
        val futureDates = when (config.partitionType) {
            PartitionProperties.PartitionType.DAILY -> (1..numPeriods).map { now.plusDays(it.toLong()) }
            PartitionProperties.PartitionType.MONTHLY -> (1..numPeriods).map { now.plusMonths(it.toLong()) }
        }

        // 각 미래 날짜에 대한 파티션 생성
        futureDates.forEach { date ->
            createSinglePartition(config.tableName, config.partitionType, date)
        }
    }

    /**
     * 특정 테이블에 단일 파티션을 생성합니다.
     */
    private fun createSinglePartition(
        tableName: String,
        partitionType: PartitionProperties.PartitionType,
        date: LocalDateTime
    ) {
        val partitionNameFormatter = partitionNameFormatters[partitionType] ?: return
        val partitionName = partitionNameFormatter(date)

        // 파티션 존재 여부 확인
        val exists = hasPartition(tableName, partitionName)
        if (exists) {
            logger.debug { "테이블 [$tableName]에 파티션 [$partitionName]이 이미 존재합니다" }
            return
        }

        try {
            // 1. p_future 파티션이 있다면 제거
            if (hasPartition(tableName, "p_future")) {
                jdbcTemplate.execute("ALTER TABLE $tableName DROP PARTITION p_future")
                logger.debug { "테이블 [$tableName]에서 p_future 파티션을 임시로 제거했습니다" }
            }

            // 2. 새 파티션 추가
            val sql = when (partitionType) {
                PartitionProperties.PartitionType.DAILY -> {
                    val nextDay = date.plusDays(1).format(DateTimeFormatter.ISO_DATE)
                    """
                    ALTER TABLE $tableName ADD PARTITION
                    (PARTITION $partitionName VALUES LESS THAN (TO_DAYS('$nextDay')))
                    """.trimIndent()
                }

                PartitionProperties.PartitionType.MONTHLY -> {
                    val nextMonth = date.plusMonths(1)
                    val nextMonthDate = LocalDate.of(nextMonth.year, nextMonth.month, 1)
                    """
                ALTER TABLE $tableName ADD PARTITION
                (PARTITION $partitionName VALUES LESS THAN (TO_DAYS('${
                        nextMonthDate.format(DateTimeFormatter.ISO_DATE)
                    }')))
                """.trimIndent()
                }
            }

            jdbcTemplate.execute(sql)
            logger.info { "테이블 [$tableName]에 파티션 [$partitionName]을 추가했습니다" }

            // 3. p_future 파티션 다시 추가
            jdbcTemplate.execute("ALTER TABLE $tableName ADD PARTITION (PARTITION p_future VALUES LESS THAN MAXVALUE)")
            logger.debug { "테이블 [$tableName]에 p_future 파티션을 다시 추가했습니다" }

        } catch (e: Exception) {
            // 오류 발생 시 p_future 복원 시도
            try {
                if (!hasPartition(tableName, "p_future")) {
                    jdbcTemplate.execute("ALTER TABLE $tableName ADD PARTITION (PARTITION p_future VALUES LESS THAN MAXVALUE)")
                    logger.debug { "오류 발생 후 테이블 [$tableName]에 p_future 파티션을 복원했습니다" }
                }
            } catch (restoreEx: Exception) {
                logger.error(restoreEx) { "테이블 [$tableName]에 p_future 파티션 복원 중 오류가 발생했습니다" }
            }

            logger.error(e) { "테이블 [$tableName]에 파티션 [$partitionName] 추가 중 오류가 발생했습니다" }
            throw e
        }
    }

    /**
     * 테이블의 초기 파티션 생성을 위한 SQL문을 생성합니다.
     */
    private fun generateInitialPartitionSQL(config: PartitionProperties.TablePartitionConfig): String {
        val now = LocalDateTime.now()
        val retentionMonths = config.retentionMonths
        val partitionType = config.partitionType

        val sql = StringBuilder()
        sql.append("ALTER TABLE ${config.tableName}\n")
        sql.append("PARTITION BY RANGE (TO_DAYS(created)) (\n")

        // 과거 파티션 추가
        when (partitionType) {
            PartitionProperties.PartitionType.DAILY -> {
                // 과거 N개월치 일간 파티션 생성
                val startDate = now.minusDays((retentionMonths * 30).toLong())
                var currentDate = startDate

                while (currentDate.isBefore(now)) {
                    val partitionName = "p_" + currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    val nextDate = currentDate.plusDays(1)
                    sql.append(
                        "    PARTITION $partitionName VALUES LESS THAN (TO_DAYS('${
                            nextDate.format(DateTimeFormatter.ISO_DATE)
                        }')),\n"
                    )
                    currentDate = nextDate
                }

                // 현재일 파티션 추가
                val todayPartitionName = "p_" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                val tomorrow = now.plusDays(1)
                sql.append(
                    "    PARTITION $todayPartitionName VALUES LESS THAN (TO_DAYS('${
                        tomorrow.format(DateTimeFormatter.ISO_DATE)
                    }')),\n"
                )

                // 미래 파티션 추가
                var futureDate = tomorrow
                val futureDays = partitionProperties.futurePeriods.daily
                for (i in 1..futureDays) {
                    val partitionName = "p_" + futureDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    val nextDate = futureDate.plusDays(1)
                    sql.append(
                        "    PARTITION $partitionName VALUES LESS THAN (TO_DAYS('${
                            nextDate.format(DateTimeFormatter.ISO_DATE)
                        }')),\n"
                    )
                    futureDate = nextDate
                }
            }

            PartitionProperties.PartitionType.MONTHLY -> {
                // 과거 N개월치 월간 파티션 생성
                val startDate = now.minusMonths(retentionMonths.toLong())
                var currentDate = LocalDate.of(startDate.year, startDate.month, 1)

                while (currentDate.isBefore(LocalDate.of(now.year, now.month, 1))) {
                    val partitionName = "p_" + currentDate.format(DateTimeFormatter.ofPattern("yyyyMM"))
                    val nextMonth = currentDate.plusMonths(1)
                    sql.append(
                        "    PARTITION $partitionName VALUES LESS THAN (TO_DAYS('${
                            nextMonth.format(DateTimeFormatter.ISO_DATE)
                        }')),\n"
                    )
                    currentDate = nextMonth
                }

                // 현재월 파티션 추가
                val currentMonthPartitionName = "p_" + now.format(DateTimeFormatter.ofPattern("yyyyMM"))
                val nextMonth = LocalDate.of(now.year, now.month, 1).plusMonths(1)
                sql.append(
                    "    PARTITION $currentMonthPartitionName VALUES LESS THAN (TO_DAYS('${
                        nextMonth.format(DateTimeFormatter.ISO_DATE)
                    }')),\n"
                )

                // 미래 파티션 추가
                var futureMonth = nextMonth
                val futureMonths = partitionProperties.futurePeriods.monthly
                for (i in 1..futureMonths) {
                    val partitionName = "p_" + futureMonth.format(DateTimeFormatter.ofPattern("yyyyMM"))
                    val nextMonthDate = futureMonth.plusMonths(1)
                    sql.append(
                        "    PARTITION $partitionName VALUES LESS THAN (TO_DAYS('${
                            nextMonthDate.format(DateTimeFormatter.ISO_DATE)
                        }')),\n"
                    )
                    futureMonth = nextMonthDate
                }
            }
        }

        // MAXVALUE 파티션 추가
        sql.append("    PARTITION p_future VALUES LESS THAN MAXVALUE\n")
        sql.append(");")

        return sql.toString()
    }

    /**
     * 테이블의 만료된 파티션을 삭제합니다.
     */
    private fun deleteExpiredPartitions(config: PartitionProperties.TablePartitionConfig) {
        logger.info { "테이블 [${config.tableName}]의 오래된 파티션을 확인합니다" }

        val retentionDate = when (config.partitionType) {
            PartitionProperties.PartitionType.DAILY -> LocalDate.now().minusDays(config.retentionMonths * 30L)
            PartitionProperties.PartitionType.MONTHLY -> LocalDate.now().minusMonths(config.retentionMonths.toLong())
        }

        // p_future 파티션은 제외하고 파티션 목록 조회
        val partitions = getPartitions(config.tableName, includeFuture = false)

        // 오래된 파티션 식별 및 제거
        partitions.forEach { partition ->
            try {
                if (isPartitionExpired(partition, retentionDate, config.partitionType)) {
                    // 아카이빙
                    archivePartitionData(config.tableName, partition)

                    // 파티션 삭제
                    deletePartition(config.tableName, partition)
                    logger.info { "테이블 [${config.tableName}]에서 오래된 파티션 [$partition]을 제거했습니다" }
                }
            } catch (e: Exception) {
                logger.error(e) { "테이블 [${config.tableName}]에서 파티션 [$partition] 제거 중 오류가 발생했습니다" }
            }
        }
    }

    /**
     * 특정 파티션이 지정된 만료일보다 오래되었는지 확인합니다.
     */
    private fun isPartitionExpired(
        partitionName: String,
        cutoffDate: LocalDate,
        partitionType: PartitionProperties.PartitionType
    ): Boolean {
        if (!partitionName.startsWith("p_")) {
            logger.debug { "파티션 [$partitionName]은 'p_'로 시작하지 않아 만료 검사에서 제외됩니다" }
            return false
        }

        if (partitionName == "p_future") {
            logger.debug { "파티션 [p_future]는 만료 검사에서 제외됩니다" }
            return false
        }

        val dateStr = partitionName.substring(2)
        logger.info { "파티션 [$partitionName] 만료 검사: 날짜 문자열 [$dateStr], 기준일 [$cutoffDate], 파티션 타입 [$partitionType]" }

        return try {
            val result = when (partitionType) {
                PartitionProperties.PartitionType.DAILY -> {
                    val partitionDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"))
                    logger.info { "파티션 [$partitionName] 날짜: [$partitionDate], 기준일: [$cutoffDate]" }
                    val isExpired = partitionDate.isBefore(cutoffDate)
                    logger.info { "파티션 [$partitionName] 만료 여부: [$isExpired]" }
                    isExpired
                }

                PartitionProperties.PartitionType.MONTHLY -> {
                    val partitionDate = LocalDate.parse(dateStr + "01", DateTimeFormatter.ofPattern("yyyyMMdd"))
                    logger.info { "파티션 [$partitionName] 날짜: [$partitionDate], 기준일: [$cutoffDate]" }
                    val isExpired = partitionDate.isBefore(cutoffDate)
                    logger.info { "파티션 [$partitionName] 만료 여부: [$isExpired]" }
                    isExpired
                }
            }
            result
        } catch (e: Exception) {
            logger.warn(e) { "파티션 [$partitionName]의 날짜 파싱 중 오류가 발생했습니다" }
            false
        }
    }

    private fun archivePartitionData(tableName: String, partitionName: String) {
        logger.debug { "테이블 [$tableName]의 파티션 [$partitionName] 아카이빙 (구현되지 않음)" }
    }

    private fun deletePartition(tableName: String, partitionName: String) {
        val sql = "ALTER TABLE $tableName DROP PARTITION $partitionName"
        jdbcTemplate.execute(sql)
        logger.info { "테이블 [$tableName]에서 파티션 [$partitionName]을 삭제했습니다" }
    }

    private fun getPartitions(tableName: String, includeFuture: Boolean = true): List<String> {
        val sqlBuilder = StringBuilder(
            """
            SELECT PARTITION_NAME FROM information_schema.PARTITIONS
            WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_NAME = ?
        """.trimIndent()
        )

        val params = mutableListOf<Any>(tableName)

        if (!includeFuture) {
            sqlBuilder.append(" AND PARTITION_NAME != 'p_future'")
        }

        sqlBuilder.append(" ORDER BY PARTITION_NAME")

        return jdbcTemplate.queryForList(
            sqlBuilder.toString(),
            String::class.java,
            *params.toTypedArray(),
        )
    }

    private fun hasPartition(tableName: String, partitionName: String? = null): Boolean {
        val sql = StringBuilder(
            """
        SELECT COUNT(*) FROM information_schema.PARTITIONS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = ?
    """.trimIndent()
        )

        val params = mutableListOf<Any>(tableName)

        if (partitionName != null) {
            sql.append(" AND PARTITION_NAME = ?")
            params.add(partitionName)
        } else {
            sql.append(" AND PARTITION_NAME IS NOT NULL")
        }

        return (jdbcTemplate.queryForObject(
            sql.toString(),
            Int::class.java,
            *params.toTypedArray(),
        ) ?: 0) > 0
    }
}