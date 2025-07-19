package kr.pincoin.api.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

/**
 * 스케줄링 작업을 위한 설정 클래스
 * - @Scheduled 어노테이션이 적용된 주기적 작업의 실행을 관리
 * - 정해진 시간에 실행되는 배치 작업, 주기적 데이터 처리 등에 사용
 * - AsyncConfig와 달리 이 설정은 일정 주기로 실행되는 작업만 관리함
 */
@Configuration
class SchedulerConfig : SchedulingConfigurer {

    /**
     * 스케줄링 작업을 위한 스레드 풀 구성
     *
     * 스케줄러 특성:
     * - 각 스레드는 하나 이상의 @Scheduled 메소드를 순차적으로 실행
     * - cron, fixedRate, fixedDelay 등으로 지정된 시간에 작업 수행
     * - 동시에 실행 가능한 스케줄 작업 수는 poolSize에 의해 제한됨
     *
     * @param taskRegistrar 스케줄된 작업 등록기
     */
    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        val threadPoolTaskScheduler = ThreadPoolTaskScheduler()
            .apply {
                // 스케줄러 스레드 풀 크기
                // - 동시에 실행 가능한 스케줄 작업 수
                // - 예: 5개 이상의 @Scheduled 작업이 동시에 실행될 경우,
                //      일부는 이전 작업 완료를 기다려야 함
                // - 너무 크게 설정하면 메모리 사용량 증가, 시스템 부하 발생
                // - 너무 작게 설정하면 스케줄 작업 지연 발생
                poolSize = 5

                // 스레드 이름 접두사 설정 - 로그에서 스케줄러 스레드 식별 용이
                setThreadNamePrefix("Scheduler-")

                // 오류 발생 시 처리자 설정 (필요시 주석 해제)
                // setErrorHandler { t -> log.error(t) { "스케줄러 작업 실행 중 오류 발생" } }

                // 스케줄러 초기화
                initialize()
            }

        // 생성된 스케줄러를 등록기에 설정
        taskRegistrar.setTaskScheduler(threadPoolTaskScheduler)
    }

    /**
     * 운영환경 스케줄러 튜닝 가이드:
     *
     * 1. 스케줄 작업 지연 발생:
     *    - 원인: 동시에 실행되는 스케줄 작업이 poolSize보다 많음
     *    - 해결: poolSize 증가 (일반적으로 동시 실행 예상 작업 수보다 20% 더 크게)
     *    - 주의: 중요 작업의 경우 별도 스케줄러 설정 고려
     *
     * 2. 작업 간 경합 발생:
     *    - 원인: 상호 의존적인 작업들이 동시에 실행됨
     *    - 해결: 작업 시작 시간 분산 또는 @Scheduled(initialDelay=) 활용
     *
     * 3. 메모리 사용량 과다:
     *    - 원인: 너무 많은 스케줄러 스레드
     *    - 해결: poolSize 감소, 작업 통합 고려
     *
     * 4. CPU 사용률 스파이크:
     *    - 원인: 동시에 많은 스케줄 작업 실행
     *    - 해결: 작업 시작 시간 분산 (cron 표현식 조정)
     *
     * 5. 장기 실행 작업으로 인한 병목:
     *    - 원인: 일부 작업이 매우 오래 실행되어 스레드 점유
     *    - 해결:
     *      a) 장기 실행 작업은 별도 스케줄러로 분리
     *      b) 작업을 더 작은 단위로 분할
     *      c) 비동기 처리 방식으로 전환 고려
     */
}