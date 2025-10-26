package com.teambind.commentserver.event.events;

import java.time.Instant;

/**
 * 도메인 이벤트 마커 인터페이스
 *
 * <p>도메인에서 발생하는 모든 이벤트는 이 인터페이스를 구현해야 합니다.
 * 이벤트 발생 시각을 추적하고, 타입 안전성을 제공합니다.
 */
public interface DomainEvent {

  /**
   * 이벤트가 발생한 시각을 반환합니다.
   *
   * @return 이벤트 발생 시각 (UTC 기준 Instant)
   */
  Instant occurredAt();
}
