package com.teambind.commentserver.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.teambind.commentserver.entity.Comment;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * CommentService 통합 테스트 (정상 시나리오만 검증)
 *
 * <p>@ActiveProfiles("test")로 H2 인메모리 DB 환경에서 실행된다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentServiceIntegrationTest {

  @Autowired private CommentService commentService;

  @DisplayName("루트 댓글 생성: depth=0, root=self")
  @Test
  void createRootComment_ok() {
    // given
    String articleId = "article-1";
    String writerId = "user-1";

    // when
    Comment root = commentService.createRootComment(articleId, writerId, "루트 댓글입니다");

    // then
    assertThat(root.getCommentId()).isNotNull();
    assertThat(root.getArticleId()).isEqualTo(articleId);
    assertThat(root.getWriterId()).isEqualTo(writerId);
    assertThat(root.getDepth()).isEqualTo(0);
    assertThat(root.getRootCommentId()).isEqualTo(root.getCommentId());
    assertThat(root.getIsDeleted()).isFalse();
  }

  @DisplayName("대댓글 생성: depth 증가, root 유지, 부모 replyCount 증가")
  @Test
  void createReply_ok() {
    // given
    Comment parent = commentService.createRootComment("article-1", "user-1", "부모 댓글");

    // when
    Comment child = commentService.createReply(parent.getCommentId(), "user-2", "자식 댓글");

    // then
    assertThat(child.getArticleId()).isEqualTo(parent.getArticleId());
    assertThat(child.getParentCommentId()).isEqualTo(parent.getCommentId());
    assertThat(child.getRootCommentId()).isEqualTo(parent.getRootCommentId());
    assertThat(child.getDepth()).isEqualTo(parent.getDepth() + 1);

    // 부모의 replyCount 증가 확인
    Comment refreshedParent = commentService.getById(parent.getCommentId()).orElseThrow();
    assertThat(refreshedParent.getReplyCount()).isEqualTo(1);
  }

  @DisplayName("아티클 전체 댓글 조회: 삭제된 댓글 제외")
  @Test
  void getAllCommentsByArticle_ok() {
    // given
    String articleId = "article-2";
    Comment c1 = commentService.createRootComment(articleId, "user-1", "c1");
    Comment c2 = commentService.createRootComment(articleId, "user-2", "c2");

    // 삭제 1건
    commentService.softDelete(c1.getCommentId(), c1.getWriterId());

    // when
    List<Comment> list = commentService.getAllCommentsByArticle(articleId);

    // then
    assertThat(list).extracting(Comment::getCommentId).containsExactly(c2.getCommentId());
  }

  @DisplayName("부모 기준 대댓글 목록 조회")
  @Test
  void getRepliesByParent_ok() {
    // given
    String articleId = "article-3";
    Comment p = commentService.createRootComment(articleId, "user-1", "부모");
    Comment r1 = commentService.createReply(p.getCommentId(), "user-2", "r1");
    Comment r2 = commentService.createReply(p.getCommentId(), "user-3", "r2");

    // when
    List<Comment> replies = commentService.getRepliesByParent(p.getCommentId());

    // then
    assertThat(replies)
        .extracting(Comment::getCommentId)
        .containsExactly(r1.getCommentId(), r2.getCommentId());
  }

  @DisplayName("루트 기준 스레드 전체 조회")
  @Test
  void getThreadByRoot_ok() {
    // given
    String articleId = "article-4";
    Comment root = commentService.createRootComment(articleId, "user-1", "루트");
    Comment c1 = commentService.createReply(root.getCommentId(), "user-2", "c1");
    Comment c2 = commentService.createReply(root.getCommentId(), "user-3", "c2");

    // when
    List<Comment> thread = commentService.getThreadByRoot(root.getRootCommentId());

    // then
    assertThat(thread)
        .extracting(Comment::getCommentId)
        .contains(root.getCommentId(), c1.getCommentId(), c2.getCommentId());
  }

  @DisplayName("내용 수정과 소프트 삭제")
  @Test
  void updateAndSoftDelete_ok() {
    // given
    Comment c = commentService.createRootComment("article-5", "user-1", "old");

    // when
    Comment updated = commentService.updateContents(c.getCommentId(), c.getWriterId(), "new");
    commentService.softDelete(updated.getCommentId(), updated.getWriterId());

    // then
    Comment fetched = commentService.getById(updated.getCommentId()).orElseThrow();
    assertThat(fetched.getContents()).isEqualTo("new");
    assertThat(fetched.getIsDeleted()).isTrue();
    assertThat(fetched.getStatus()).isEqualTo(Comment.CommentStatus.DELETED);
  }
}
