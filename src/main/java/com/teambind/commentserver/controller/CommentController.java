package com.teambind.commentserver.controller;

import com.teambind.commentserver.dto.CommentResponse;
import com.teambind.commentserver.dto.CreateReplyRequest;
import com.teambind.commentserver.dto.CreateRootCommentRequest;
import com.teambind.commentserver.dto.UpdateCommentRequest;
import com.teambind.commentserver.entity.Comment;
import com.teambind.commentserver.exceptions.CustomException;
import com.teambind.commentserver.exceptions.ErrorCode;
import com.teambind.commentserver.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  // 루트 댓글 생성
  @PostMapping
  public ResponseEntity<CommentResponse> createRoot(
      @Valid @RequestBody CreateRootCommentRequest req) {
    Comment saved =
        commentService.createRootComment(req.getArticleId(), req.getWriterId(), req.getContents());
    return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(saved));
  }

  // 대댓글 생성
  @PostMapping("/{parentId}/replies")
  public ResponseEntity<CommentResponse> createReply(
      @PathVariable String parentId, @Valid @RequestBody CreateReplyRequest req) {
    Comment saved = commentService.createReply(parentId, req.getWriterId(), req.getContents());
    return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(saved));
  }

  // 특정 아티클의 삭제되지 않은 전체 댓글 조회
  @GetMapping("/article/{articleId}")
  public ResponseEntity<List<CommentResponse>> getByArticle(@PathVariable String articleId) {
    List<CommentResponse> list =
        commentService.getAllCommentsByArticle(articleId).stream()
            .map(CommentResponse::from)
            .toList();
    return ResponseEntity.ok(list);
  }

  // 특정 부모 댓글의 자식(대댓글) 조회
  @GetMapping("/{parentId}/replies")
  public ResponseEntity<List<CommentResponse>> getReplies(@PathVariable String parentId) {
    List<CommentResponse> list =
        commentService.getRepliesByParent(parentId).stream().map(CommentResponse::from).toList();
    return ResponseEntity.ok(list);
  }

  // 루트 댓글 기준 스레드 전체 조회
  @GetMapping("/thread/{rootId}")
  public ResponseEntity<List<CommentResponse>> getThread(@PathVariable String rootId) {
    List<CommentResponse> list =
        commentService.getThreadByRoot(rootId).stream().map(CommentResponse::from).toList();
    return ResponseEntity.ok(list);
  }

  // 단건 조회
  @GetMapping("/{id}")
  public ResponseEntity<CommentResponse> getById(@PathVariable String id) {
    Comment c =
        commentService
            .getById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    return ResponseEntity.ok(CommentResponse.from(c));
  }

  // 내용 수정
  @PatchMapping("/{id}")
  public ResponseEntity<CommentResponse> update(
      @PathVariable String id, @Valid @RequestBody UpdateCommentRequest req) {
    Comment updated = commentService.updateContents(id, req.getWriterId(), req.getContents());
    return ResponseEntity.ok(CommentResponse.from(updated));
  }

  // 소프트 삭제 (작성자 본인만)
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable String id, @RequestParam("writerId") String writerId) {
    commentService.softDelete(id, writerId);
    return ResponseEntity.noContent().build();
  }
}
