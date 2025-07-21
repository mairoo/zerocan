package kr.pincoin.api.infra.common.lock

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*
import kotlin.text.isEmpty
import kotlin.text.toByteArray
import kotlin.text.trimIndent
import kotlin.to

/**
 * Redis를 이용한 분산 락 유틸리티 클래스
 * 여러 서버 인스턴스에서 중복 작업 방지가 필요한 초기화 작업 등에 사용
 */
@Component
class RedisDistributedLock(
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 분산 락을 획득합니다.
     *
     * @param lockKey 락 키 이름
     * @param timeout 락 유지 시간
     * @return Pair(획득 성공 여부, 락 식별 값)
     */
    fun acquire(lockKey: String, timeout: Duration): Pair<Boolean, String> {
        val lockValue = UUID.randomUUID().toString()

        try {
            val acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, timeout)

            return (acquired == true) to lockValue
        } catch (e: Exception) {
            logger.error(e) { "분산 락 획득 중 오류 발생: $lockKey, ${e.message}" }
            return false to ""
        }
    }

    /**
     * 분산 락을 해제합니다.
     * 자신이 획득한 락만 해제할 수 있도록 lockValue를 확인합니다.
     *
     * @param lockKey 락 키 이름
     * @param lockValue 락 획득 시 반환된 락 식별 값
     * @return 락 해제 성공 여부
     */
    fun release(lockKey: String, lockValue: String): Boolean {
        if (lockValue.isEmpty()) {
            return false
        }

        try {
            // 자신이 설정한 락인지 확인 후 삭제 (Lua 스크립트로 원자적 연산)
            val script = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
            """.trimIndent()

            val result = redisTemplate.execute<Long> { connection ->
                connection.scriptingCommands().eval(
                    script.toByteArray(),
                    ReturnType.INTEGER,
                    1,
                    lockKey.toByteArray(),
                    lockValue.toByteArray()
                )
            }

            val success = result == 1L
            if (success) {
                logger.debug { "분산 락 해제 완료: $lockKey" }
            } else {
                logger.warn { "분산 락 해제 실패: $lockKey (락이 이미 해제되었거나 다른 인스턴스에 의해 획득됨)" }
            }

            return success
        } catch (e: Exception) {
            logger.error(e) { "분산 락 해제 중 오류 발생: $lockKey, ${e.message}" }
            return false
        }
    }

    /**
     * 작업 완료 상태를 확인합니다.
     *
     * @param taskKey 작업 식별 키
     * @return 작업 완료 여부
     */
    fun isTaskCompleted(taskKey: String): Boolean {
        val completionKey = "$taskKey:completed"
        return redisTemplate.hasKey(completionKey)
    }

    /**
     * 작업 완료 상태를 설정합니다.
     *
     * @param taskKey 작업 식별 키
     * @param ttl 완료 상태 유지 시간
     * @return 설정 성공 여부
     */
    fun markTaskAsCompleted(taskKey: String, ttl: Duration): Boolean {
        val completionKey = "$taskKey:completed"
        return try {
            redisTemplate.opsForValue().set(completionKey, "true", ttl)
            true
        } catch (e: Exception) {
            logger.error(e) { "작업 완료 상태 설정 중 오류 발생: $taskKey, ${e.message}" }
            false
        }
    }

    /**
     * 작업 완료 상태를 확인하고, 완료되지 않은 경우에만 락을 획득하여 작업을 실행합니다.
     * 작업 완료 후 완료 상태를 설정합니다.
     *
     * @param taskKey 작업 식별 키
     * @param lockTimeout 락 유지 시간
     * @param completionTtl 완료 상태 유지 시간
     * @param action 실행할 작업
     * @return 작업 실행 여부와 결과
     */
    fun <T> withCompletionTracking(
        taskKey: String,
        lockTimeout: Duration,
        completionTtl: Duration,
        action: () -> T
    ): Pair<Boolean, T?> {
        // 1. 먼저 작업 완료 상태 확인
        if (isTaskCompleted(taskKey)) {
            logger.info { "작업 [$taskKey]이(가) 이미 완료되었습니다." }
            return false to null
        }

        // 2. 락 획득 시도
        val (acquired, lockValue) = acquire(taskKey, lockTimeout)

        if (!acquired) {
            logger.info { "분산 락 획득 실패: $taskKey" }
            return false to null
        }

        return try {
            logger.info { "분산 락 획득 성공: $taskKey" }

            // 3. 락 획득 후 다시 한번 완료 상태 확인 (경합 상태 방지)
            if (isTaskCompleted(taskKey)) {
                logger.info { "락 획득 후 확인 결과, 작업 [$taskKey]이(가) 이미 완료되었습니다." }
                return false to null
            }

            // 4. 작업 실행
            val result = action()

            // 5. 작업 완료 표시
            markTaskAsCompleted(taskKey, completionTtl)

            true to result
        } catch (e: Exception) {
            logger.error(e) { "분산 락 내 작업 실행 중 오류 발생: $taskKey, ${e.message}" }
            throw e
        } finally {
            release(taskKey, lockValue)
        }
    }
}