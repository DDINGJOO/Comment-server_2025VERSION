package com.teambind.commentserver.event.publish;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.commentserver.event.events.CommentCreatedEvent;
import com.teambind.commentserver.event.events.CommentDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 도메인 이벤트 발행 서비스
 *
 * <p>댓글 도메인에서 발생하는 이벤트를 Kafka를 통해 발행합니다.
 * 메서드 네이밍은 명확한 동사 형태(publish~)를 사용하여 의도를 명확히 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;

  /**
   * 댓글 생성 이벤트 발행
   *
   * @param event 댓글 생성 이벤트
   */
  public void publishCommentCreated(CommentCreatedEvent event) {
    publish("comment-created", event);
  }

  /**
   * 댓글 삭제 이벤트 발행
   *
   * @param event 댓글 삭제 이벤트
   */
  public void publishCommentDeleted(CommentDeletedEvent event) {
    publish("comment-deleted", event);
  }

  /**
   * Kafka 토픽에 이벤트를 발행하는 내부 메서드
   *
   * @param topic Kafka 토픽명
   * @param message 발행할 메시지 객체
   * @throws RuntimeException JSON 직렬화 실패 시
   */
  private void publish(String topic, Object message) {
    try {
      String json = objectMapper.writeValueAsString(message);
      kafkaTemplate.send(topic, json);
      log.debug("Published event to topic={}, message={}", topic, json);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize event to JSON: topic={}, message={}", topic, message, e);
      throw new RuntimeException("Failed to serialize message to JSON", e);
    }
  }
}
