package com.teambind.commentserver.event.publish;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.commentserver.event.events.CommentCreatedEvent;
import com.teambind.commentserver.event.events.CommentDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisher {
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;

  public void CommentCreateEvent(CommentCreatedEvent event) {
    publish("comment-created", event);
  }

  public void CommentDeleteEvent(CommentDeletedEvent event) {
    publish("comment-deleted", event);
  }

  private void publish(String topic, Object message) {
    try {
      String json = objectMapper.writeValueAsString(message);
      // KafkaTemplate의 제네릭은 Object로 되어 있어도 String을 보낼 수 있음.
      kafkaTemplate.send(topic, json);
    } catch (JsonProcessingException e) {
      // 역직렬화 실패 시 적절한 로깅/예외 처리
      throw new RuntimeException("Failed to serialize message to JSON", e);
    }
  }
}
