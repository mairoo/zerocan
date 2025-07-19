package kr.pincoin.api.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * 비동기 처리를 위한 설정 클래스
 * - 이벤트 처리 및 비동기 작업에 사용되는 스레드 풀 구성
 * - @EnableAsync: 애플리케이션에서 비동기 메소드 실행을 활성화
 * - 로그인 로깅, 이체 로깅 등의 백그라운드 작업에 사용
 */
@Configuration
@EnableAsync
class AsyncConfig {

    /**
     * 비동기 이벤트 처리를 위한 스레드 풀 구성
     *
     * 스레드 풀 매개변수 설명:
     * - corePoolSize: 기본적으로 유지되는 스레드 수. 작업이 없어도 이 개수만큼 유지됨
     * - maxPoolSize: 최대 스레드 수. 큐가 가득 찼을 때만 core 이상 생성됨
     * - queueCapacity: 작업 큐 용량. 코어 스레드가 모두 사용 중일 때 큐에 작업 대기
     *
     * 동작 방식:
     * 1. 작업 요청 시 코어 스레드(5개)가 처리
     * 2. 코어 스레드가 모두 사용 중이면 큐(25개)에 작업 대기
     * 3. 큐가 가득 차면 추가 스레드 생성(최대 10개까지)
     * 4. 모든 스레드 사용 중 + 큐 가득 참 = 추가 작업 거부(RejectedExecutionException)
     *
     * @return 구성된 Executor 객체
     */
    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()

        // 기본 스레드 수 - 항상 유지되는 스레드 수
        // 낮추면 메모리 사용량 감소, 높이면 동시 처리량 증가
        executor.corePoolSize = 5

        // 최대 스레드 수 - 큐가 가득 찼을 때 추가 생성되는 최대 한도
        // CPU 코어 수의 1-2배 정도가 적절. 과도하게 높으면 CPU 경합 발생
        executor.maxPoolSize = 10

        // 작업 큐 용량 - 스레드가 모두 사용 중일 때 대기열 크기
        // 너무 크면 메모리 사용량 증가, 작업 처리 지연. 너무 작으면 작업 거부 빈발
        executor.queueCapacity = 25

        // 스레드 이름 접두사 - 로그에서 비동기 이벤트 스레드 식별 용이
        executor.setThreadNamePrefix("AsyncEvent-")

        // 초기화 - 설정 적용 및 스레드 풀 준비
        executor.initialize()

        return executor
    }

    /**
     * 운영환경 장애 상황별 튜닝 가이드:
     *
     * 1. CPU 사용률 과다:
     *    - 원인: 스레드 수가 너무 많아 CPU 리소스 경합
     *    - 해결: maxPoolSize 감소 (CPU 코어 수 기준 조정)
     *
     * 2. 작업 처리 지연/타임아웃:
     *    - 원인: 큐 크기가 너무 크거나 스레드 부족
     *    - 해결: corePoolSize 증가, queueCapacity 감소
     *
     * 3. 메모리 사용량 과다:
     *    - 원인: 너무 많은 스레드 또는 큰 큐
     *    - 해결: corePoolSize와 maxPoolSize 감소
     *
     * 4. 작업 거부(RejectedExecutionException) 빈번:
     *    - 원인: 스레드 풀과 큐 모두 포화
     *    - 해결: maxPoolSize 또는 queueCapacity 증가
     *
     * 5. 이벤트 처리 지연:
     *    - 원인: 동시 이벤트 많음
     *    - 해결: corePoolSize 증가
     */
}