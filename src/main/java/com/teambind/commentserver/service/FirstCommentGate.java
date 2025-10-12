package com.teambind.commentserver.service;

/**
 * 첫 댓글 이벤트 발행 여부를 결정하는 게이트 인터페이스.
 *
 * <p>기본 아이디어: 유저×아티클 조합에 대해 일정 기간(예: 2일) 동안만 "첫 댓글"로 간주하고 이벤트를 1회만 발행하도록 제어한다. 구현체는 Redis 등 외부 캐시를
 * 사용할 수 있고, 장애 시 폴백 전략을 가질 수 있다.
 */
public interface FirstCommentGate {

  /**
   * 주어진 아티클/작성자 조합이 TTL 윈도우 내에서 "첫 댓글"로 인정되는지 여부를 반환한다.
   *
   * @param articleId 아티클 ID
   * @param writerId 작성자 유저 ID
   * @return true 이면 이번 요청이 TTL 윈도우 내 첫 댓글이므로 이벤트를 발행해야 함. false 이면 스킵.
   */
  boolean isFirstWithinWindow(String articleId, String writerId);
}
