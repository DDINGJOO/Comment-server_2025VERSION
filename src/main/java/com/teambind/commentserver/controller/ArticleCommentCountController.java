package com.teambind.commentserver.controller;

import com.teambind.commentserver.service.ArticleCommentCountService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 배치용: 여러 articleId에 대한 commentCount를 한 번에 조회하는 컨트롤러 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Validated
public class ArticleCommentCountController {

  private final ArticleCommentCountService articleCommentCountService;

  /**
   * 여러 articleId에 대한 댓글 수를 반환합니다. body: ["article-1", "article-2", ...] 반환: { "article-1": 10,
   * "article-2": 0, ... }
   */
  @PostMapping("/articles/counts")
  public ResponseEntity<Map<String, Integer>> getCountsForArticles(
      @RequestBody List<String> articleIds) {
    if (articleIds == null || articleIds.isEmpty()) {
      return ResponseEntity.ok(Map.of());
    }

    Map<String, Integer> counts = articleCommentCountService.getCountsForArticles(articleIds);
    return ResponseEntity.ok(counts);
  }
}
