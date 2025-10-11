package com.teambind.commentserver.service;

import com.teambind.commentserver.dto.CommentResponse;
import com.teambind.commentserver.entity.Comment;
import java.util.List;
import java.util.Optional;

/**
 * 댓글 서비스 인터페이스
 *
 * <p>정상 시나리오 중심의 기능을 정의한다. (예외 케이스 처리는 추후 보강)
 */
public interface CommentService {

  /** 루트 댓글 생성 (depth=0, rootCommentId=self) */
  Comment createRootComment(String articleId, String writerId, String contents);

  /** 부모 댓글에 대한 대댓글 생성 (depth=부모+1) */
  Comment createReply(String parentCommentId, String writerId, String contents);

  /** 특정 아티클의 삭제되지 않은 전체 댓글 조회 (생성일 오름차순) */
  List<Comment> getAllCommentsByArticle(String articleId);

  List<CommentResponse> getCommentsByArticleByVisibleCount(
      String articleId, int page, int pageSize);

  /** 특정 부모 댓글의 자식(대댓글) 조회 */
  List<Comment> getRepliesByParent(String parentCommentId);

  /** 루트 댓글 기준 스레드 전체 조회 */
  List<Comment> getThreadByRoot(String rootCommentId);

  /** 단건 조회 */
  Optional<Comment> getById(String commentId);

  /** 소프트 삭제 (작성자 본인만 가능) */
  void softDelete(String commentId, String requesterId);

  /** 댓글 내용 수정 (작성자 본인만 가능) */
  Comment updateContents(String commentId, String requesterId, String newContents);
}
