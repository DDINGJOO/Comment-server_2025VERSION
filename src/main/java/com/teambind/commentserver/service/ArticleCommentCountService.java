package com.teambind.commentserver.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** article_comment_counts 프로젝션을 관리하는 서비스 */
public interface ArticleCommentCountService {
  Optional<Integer> getCount(String articleId);

  /** article_comment_counts 레코드가 없으면 생성하고 delta를 더합니다. delta는 음수일 수 있습니다. */
  void upsertAndAdd(String articleId, int delta);

  /** 원자적으로 +1 */
  void increment(String articleId);

  /** 원자적으로 -1 (최소 0 유지) */
  void decrement(String articleId);

  /** 보정용으로 명시적 세팅 */
  void setCount(String articleId, int count);

  /** 배치(여러 articleId)에 대해 id -> commentCount 매핑을 반환합니다. 존재하지 않는 articleId는 0으로 채워 반환합니다. */
  Map<String, Integer> getCountsForArticles(List<String> articleIds);
}
