package com.teambind.commentserver.service.impl;

import com.teambind.commentserver.repository.CommentRepository;
import com.teambind.commentserver.service.FirstCommentGate;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 첫 댓글 게이트 구현.
 *
 * <p>동작:
 * - 키: c:first:v1:{articleId}:{userId}
 * - 연산: SETNX + EX 2일 (opsForValue().setIfAbsent)
 * - 성공(true)일 때만 "첫 댓글"로 간주하여 이벤트를 발행하도록 상위 레이어에서 처리
 *
 * <p>장애 시 폴백:
 * - Redis 예외 발생 시 안전하게 false 를 반환하여 이벤트 중복 발행을 방지하거나,
 *   필요시 DB 조회로 폴백하는 확장 포인트를 남겨두었다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisFirstCommentGate implements FirstCommentGate {

  // 2일 TTL (요구사항에 맞춤)
  private static final Duration TTL = Duration.ofDays(2);
  private final StringRedisTemplate redisTemplate;
  private final CommentRepository commentRepository; // 폴백 용도(현재는 사용하지 않지만 확장 포인트)

  @Override
  public boolean isFirstWithinWindow(String articleId, String writerId) {
    //  유저×아티클 조합에 대해 2일 동안만 첫 댓글로 인정하기 위해 Redis SETNX 사용
    String key = buildKey(articleId, writerId);
    try {
      Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);
      return Boolean.TRUE.equals(success);
    } catch (DataAccessException ex) {
      //  Redis 장애 시 이벤트 중복 발행을 피하기 위해 기본은 false 반환
      // 필요 시 아래 주석 해제하여 DB 폴백 로직 추가 가능
      // long count = commentRepository.countByArticleIdAndWriterIdAndIsDeletedFalseAndStatus(articleId, writerId, com.teambind.commentserver.entity.Comment.CommentStatus.ACTIVE);
      // return count == 0;
      log.warn("[FirstCommentGate] Redis 예외 발생으로 스킵 articleId={}, writerId={}, err={}", articleId, writerId, ex.getMessage());
      return false;
    }
  }

  private String buildKey(String articleId, String writerId) {
    return String.format("c:first:v1:%s:%s", articleId, writerId);
  }
}
