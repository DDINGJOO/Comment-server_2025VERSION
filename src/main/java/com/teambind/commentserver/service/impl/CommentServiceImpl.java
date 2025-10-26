package com.teambind.commentserver.service.impl;

import com.teambind.commentserver.dto.CommentResponse;
import com.teambind.commentserver.entity.Comment;
import com.teambind.commentserver.entity.Comment.CommentStatus;
import com.teambind.commentserver.event.events.CommentCreatedEvent;
import com.teambind.commentserver.event.events.CommentDeletedEvent;
import com.teambind.commentserver.event.publish.EventPublisher;
import com.teambind.commentserver.exceptions.CustomException;
import com.teambind.commentserver.exceptions.ErrorCode;
import com.teambind.commentserver.repository.CommentRepository;
import com.teambind.commentserver.service.ArticleCommentCountService;
import com.teambind.commentserver.service.CommentService;
import com.teambind.commentserver.service.FirstCommentGate;
import com.teambind.commentserver.utils.primarykey.PrimaryKeyProvider;
import java.util.*;
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
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

  private final CommentRepository commentRepository; // 댓글 저장소 (JPA)
  private final PrimaryKeyProvider primaryKeyProvider; // 고유 키 발급기 (Snowflake)
  private final ArticleCommentCountService articleCommentCountService; // 추가: 아티클별 카운트 서비스
  private final EventPublisher eventPublisher; // 이벤트 퍼블리셔
  private final FirstCommentGate firstCommentGate; // 첫 댓글 이벤트 발행을 제어하는 Redis 게이트

  @Override
  @Transactional
  public Comment createRootComment(String articleId, String writerId, String contents) {
    // 루트 댓글 생성 팩토리 메서드 사용
    String id = primaryKeyProvider.generateKey();
    Comment comment = Comment.createRoot(id, articleId, writerId, contents);

    Comment saved = commentRepository.save(comment);

    // 게시글 댓글 수 증가
    articleCommentCountService.increment(articleId);

    // 첫 댓글 이벤트 발행 (필요 시)
    publishFirstCommentEventIfNeeded(articleId, writerId);

    return saved;
  }

  @Override
  @Transactional
  public Comment createReply(String parentCommentId, String writerId, String contents) {
    // 부모 댓글 조회
    Comment parent =
        commentRepository
            .findById(parentCommentId)
            .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));

    // 답글 생성 팩토리 메서드 사용
    String id = primaryKeyProvider.generateKey();
    Comment reply = Comment.createReply(id, parent, writerId, contents);

    // 부모의 답글 수를 증가시킨다 (연관관계 편의 메서드)
    parent.addReply();
    // parent는 JPA dirty checking으로 자동 업데이트됨

    Comment savedReply = commentRepository.save(reply);

    // 게시글 댓글 수 증가
    articleCommentCountService.increment(parent.getArticleId());

    // 첫 댓글 이벤트 발행 (필요 시)
    publishFirstCommentEventIfNeeded(parent.getArticleId(), writerId);

    return savedReply;
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
    return commentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentCommentId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Comment> getThreadByRoot(String rootCommentId) {
    // 루트 댓글 id 기준으로 스레드 전체를 조회
    return commentRepository.findByRootCommentIdOrderByCreatedAtAsc(rootCommentId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Comment> getById(String commentId) {
    return commentRepository.findById(commentId);
  }

  @Override
  @Transactional
  public void softDelete(String commentId, String requesterId) {
    // 소프트 삭제: 작성자 본인만 가능
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

    if (!comment.isOwnedBy(requesterId)) {
      throw new CustomException(ErrorCode.NOT_COMMENT_OWNER);
    }

    if (Boolean.TRUE.equals(comment.getIsDeleted())) {
      return; // 이미 삭제된 경우 아무 작업도 하지 않음
    }

    comment.markDeleted();
    // comment는 JPA dirty checking으로 자동 업데이트됨

    // 이벤트: 해당 게시글의 남은 활성 댓글 수가 0이면(마지막 댓글 삭제) 이벤트 발행
    long remain =
        commentRepository.countByArticleIdAndIsDeletedFalseAndStatus(
            comment.getArticleId(), CommentStatus.ACTIVE);
    if (remain == 0) {
      eventPublisher.publishCommentDeleted(
          CommentDeletedEvent.builder()
              .writerId(comment.getWriterId())
              .articleId(comment.getArticleId())
              .build());
    }
  }

  @Override
  @Transactional
  public Comment updateContents(String commentId, String requesterId, String newContents) {
    // 댓글 내용을 갱신한다. 작성자 본인만 가능
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

    if (!comment.isOwnedBy(requesterId)) {
      throw new CustomException(ErrorCode.NOT_COMMENT_OWNER);
    }

    try {
      comment.updateContents(newContents);
    } catch (IllegalArgumentException e) {
      throw new CustomException(ErrorCode.CONTENTS_REQUIRED);
    }

    return comment;
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

  /**
   * 루트 단위로 "화면에 표시되는 댓글 수(루트 + 자식 합)" 기준 페이징. page: 0-based, pageSize: 한 화면에 보이는 총 댓글 개수 (루트 포함)
   */
  @Transactional(readOnly = true)
  public List<CommentResponse> getCommentsByArticleByVisibleCount(
      String articleId, int page, int pageSize) {
    long prevLimit = (long) page * pageSize;
    long currLimit = (long) (page + 1) * pageSize;

    List<String> rootIds = commentRepository.findRootIdsForPage(articleId, prevLimit, currLimit);
    if (rootIds == null || rootIds.isEmpty()) {
      return Collections.emptyList();
    }

    // 루트 + 자식들을 한 번에 조회
    List<Comment> rows = commentRepository.findRootsAndChildrenByRootIds(articleId, rootIds);

    // 먼저 모든 댓글을 Map으로 변환 (O(1) 조회를 위해)
    Map<String, Comment> commentMap =
        rows.stream().collect(Collectors.toMap(Comment::getCommentId, Function.identity()));

    // 루트 순서를 보장하기 위해 LinkedHashMap 사용
    Map<String, CommentResponse> rootMap = new LinkedHashMap<>();
    for (String rootId : rootIds) {
      rootMap.put(rootId, null); // placeholder to preserve order
    }

    for (Comment c : rows) {
      if (c.getDepth() == 0) {
        rootMap.put(c.getCommentId(), CommentResponse.from(c));
      } else {
        String rootId = c.getRootCommentId();
        CommentResponse rootDto = rootMap.get(rootId);
        if (rootDto == null) {
          // 루트 DTO가 아직 placeholder인 경우, commentMap에서 조회 (N+1 문제 해결)
          Comment rootComment = commentMap.get(rootId);
          if (rootComment != null) {
            rootDto = CommentResponse.from(rootComment);
            rootMap.put(rootId, rootDto);
          } else {
            // 루트가 없는 경우 (데이터 정합성 문제) - 로그 남기고 스킵
            continue;
          }
        }
        rootDto.addReply(CommentResponse.from(c));
      }
    }

    // 결과 리스트(루트 순서대로)
    return rootMap.values().stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  /**
   * 첫 댓글 생성 이벤트를 발행합니다. (필요한 경우에만)
   *
   * @param articleId 게시글 ID
   * @param writerId 작성자 ID
   */
  private void publishFirstCommentEventIfNeeded(String articleId, String writerId) {
    // 이벤트: 해당 사용자가 해당 게시글에 단 첫 댓글인지(2일 윈도우) Redis로 판단하여 발행
    if (firstCommentGate.isFirstWithinWindow(articleId, writerId)) {
      eventPublisher.publishCommentCreated(
          CommentCreatedEvent.builder()
              .writerId(writerId)
              .articleId(articleId)
              .build());
    }
  }
}
