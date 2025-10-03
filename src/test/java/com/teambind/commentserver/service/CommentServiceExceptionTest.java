package com.teambind.commentserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teambind.commentserver.exceptions.CustomException;
import com.teambind.commentserver.exceptions.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentServiceExceptionTest {

  @Autowired private CommentService commentService;

  @DisplayName("대댓글 생성시 부모가 없으면 커스텀 예외 발생")
  @Test
  void createReply_parentNotFound_throws() {
    // given
    String invalidParentId = "not-exist";

    // when & then
    assertThatThrownBy(() -> commentService.createReply(invalidParentId, "user-1", "자식"))
        .isInstanceOf(CustomException.class)
        .hasMessage(ErrorCode.PARENT_COMMENT_NOT_FOUND.getMessage())
        .satisfies(
            ex ->
                assertThat(((CustomException) ex).getErrorcode())
                    .isEqualTo(ErrorCode.PARENT_COMMENT_NOT_FOUND));
  }

  @DisplayName("소프트 삭제시 존재하지 않는 댓글이면 커스텀 예외 발생")
  @Test
  void softDelete_commentNotFound_throws() {
    // given
    String invalidId = "nope";

    // when & then
    assertThatThrownBy(() -> commentService.softDelete(invalidId))
        .isInstanceOf(CustomException.class)
        .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage())
        .satisfies(
            ex ->
                assertThat(((CustomException) ex).getErrorcode())
                    .isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
  }

  @DisplayName("내용 수정시 존재하지 않는 댓글이면 커스텀 예외 발생")
  @Test
  void updateContents_commentNotFound_throws() {
    // given
    String invalidId = "missing";

    // when & then
    assertThatThrownBy(() -> commentService.updateContents(invalidId, "any"))
        .isInstanceOf(CustomException.class)
        .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage())
        .satisfies(
            ex ->
                assertThat(((CustomException) ex).getErrorcode())
                    .isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
  }

  @DisplayName("내용 수정시 내용이 null이면 CONTENTS_REQUIRED 예외 발생")
  @Test
  void updateContents_null_throws() {
    // given
    String dummyId = "dummy"; // 존재 여부와 무관하게 contents null 체크가 먼저 수행되지 않음에 유의

    // when & then
    assertThatThrownBy(() -> commentService.updateContents(dummyId, null))
        .isInstanceOf(CustomException.class)
        .hasMessage(ErrorCode.CONTENTS_REQUIRED.getMessage())
        .satisfies(
            ex ->
                assertThat(((CustomException) ex).getErrorcode())
                    .isEqualTo(ErrorCode.CONTENTS_REQUIRED));
  }

  @DisplayName("내용 수정시 내용이 공백이면 CONTENTS_REQUIRED 예외 발생")
  @Test
  void updateContents_blank_throws() {
    // given
    String dummyId = "dummy";

    // when & then
    assertThatThrownBy(() -> commentService.updateContents(dummyId, "  \t\n"))
        .isInstanceOf(CustomException.class)
        .hasMessage(ErrorCode.CONTENTS_REQUIRED.getMessage())
        .satisfies(
            ex ->
                assertThat(((CustomException) ex).getErrorcode())
                    .isEqualTo(ErrorCode.CONTENTS_REQUIRED));
  }
}
