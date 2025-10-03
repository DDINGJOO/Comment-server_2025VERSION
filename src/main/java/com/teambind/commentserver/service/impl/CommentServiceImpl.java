package com.teambind.commentserver.service.impl;

import com.teambind.commentserver.entity.Comment;
import com.teambind.commentserver.repository.CommentRepository;
import com.teambind.commentserver.service.CommentService;
import com.teambind.commentserver.utils.primarykey.KeyProvider;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 서비스 구현체
 *
 * <p>요구사항: - 아티클에 대한 전체 하위 댓글 조회 - 부모 댓글을 통해 대댓글(자식) 조회 - 루트 기준 스레드 전체 조회 - 소프트 삭제 처리 - 내용 수정 등 기본
 * 기능 제공
 *
 * <p>주석은 한국어로 작성되어 있으며, 테스트는 정상 시나리오만을 다룹니다.
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

  private final CommentRepository commentRepository; // 댓글 저장소 (JPA)
  private final KeyProvider keyProvider; // 고유 키 발급기 (Snowflake)

  @Override
  @Transactional
  public Comment createRootComment(String articleId, String writerId, String contents) {
    // 루트 댓글은 자기 자신을 rootCommentId로 설정하고 depth=0 으로 저장한다.
    String id = keyProvider.generateKey();
    Comment comment =
        Comment.builder()
            .commentId(id)
            .articleId(articleId)
            .writerId(writerId)
            .contents(contents)
            .depth(0)
            .rootCommentId(id)
            .build();

    return commentRepository.save(comment);
  }

  @Override
  @Transactional
  public Comment createReply(String parentCommentId, String writerId, String contents) {
    // 부모 댓글을 조회하고, 동일 아티클로 depth를 +1 하여 자식 댓글을 생성한다.
    Comment parent =
        commentRepository
            .findById(parentCommentId)
            .orElseThrow(
                () -> new IllegalArgumentException("Parent comment not found: " + parentCommentId));

    String id = keyProvider.generateKey();

    // 부모의 rootCommentId가 없으면 부모 자신이 루트이므로 부모 id를 사용한다.
    Comment reply =
        Comment.builder()
            .commentId(id)
            .articleId(parent.getArticleId())
            .writerId(writerId)
            .contents(contents)
            .parentCommentId(parent.getCommentId())
            .rootCommentId(
                parent.getRootCommentId() == null
                    ? parent.getCommentId()
                    : parent.getRootCommentId())
            .depth((parent.getDepth() == null ? 0 : parent.getDepth()) + 1)
            .build();

    // 부모의 답글 수를 증가시킨다.
    parent.incrementReplyCount();
    commentRepository.save(parent);

    return commentRepository.save(reply);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Comment> getAllCommentsByArticle(String articleId) {
    // 삭제되지 않은 댓글만 생성일 기준 오름차순으로 반환
    return commentRepository.findByArticleIdAndIsDeletedFalseOrderByCreatedAtAsc(articleId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Comment> getRepliesByParent(String parentCommentId) {
    // 특정 부모의 자식 댓글(대댓글)만 조회
    return commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(
        parentCommentId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Comment> getThreadByRoot(String rootCommentId) {
    // 루트 댓글 id 기준으로 스레드 전체를 조회
    return commentRepository.findByRootCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(rootCommentId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Comment> getById(String commentId) {
    return commentRepository.findById(commentId);
  }

  @Override
  @Transactional
  public void softDelete(String commentId) {
    // 소프트 삭제: 플래그 및 상태만 변경한다. (실제 삭제 아님)
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

    if (Boolean.TRUE.equals(comment.getIsDeleted())) {
      return; // 이미 삭제된 경우 아무 작업도 하지 않음
    }

    comment.markDeleted();
    commentRepository.save(comment);
  }

  @Override
  @Transactional
  public Comment updateContents(String commentId, String newContents) {
    // 댓글 내용을 갱신한다.
    Objects.requireNonNull(newContents, "newContents must not be null");
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
    comment.setContents(newContents);
    return commentRepository.save(comment);
  }

  // (선택) 간단한 스레드 정렬 유틸: 부모가 먼저, 자식이 뒤에 오도록 정렬
  private List<Comment> orderAsThread(List<Comment> comments) {
    Map<String, Comment> map =
        comments.stream().collect(Collectors.toMap(Comment::getCommentId, Function.identity()));

    // createdAt, depth 순으로 결정적 정렬을 보장한다.
    return comments.stream()
        .sorted(
            Comparator.comparing(
                    Comment::getRootCommentId, Comparator.nullsFirst(String::compareTo))
                .thenComparing(Comment::getCreatedAt))
        .toList();
  }
}
