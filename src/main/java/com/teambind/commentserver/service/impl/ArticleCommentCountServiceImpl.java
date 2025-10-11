package com.teambind.commentserver.service.impl;

import com.teambind.commentserver.entity.ArticleCommentCount;
import com.teambind.commentserver.repository.ArticleCommentCountRepository;
import com.teambind.commentserver.service.ArticleCommentCountService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ArticleCommentCount 관련 비즈니스 로직 구현.
 *
 * <p>동작 원칙: - 가급적 repository의 원자적 쿼리(upsertAndAdd / increment / decrement)를 사용하여 경쟁 조건을 줄임. - 레코드
 * 미존재 시 upsertAndAdd 로 생성 처리.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleCommentCountServiceImpl implements ArticleCommentCountService {

  private final ArticleCommentCountRepository repository;

  @Override
  @Transactional(readOnly = true)
  public Optional<Integer> getCount(String articleId) {
    return repository.findById(articleId).map(ArticleCommentCount::getCommentCount);
  }

  @Override
  @Transactional
  public void upsertAndAdd(String articleId, int delta) {
    if (delta == 0) {
      return;
    }
    try {
      int updated = repository.upsertAndAdd(articleId, delta);
      if (updated == 0) {
        // upsertAndAdd 쿼리는 영향받은 row 수를 반환. 0이면 드물게 실패했을 수 있으므로 폴백
        repository.save(
            ArticleCommentCount.builder()
                .articleId(articleId)
                .commentCount(Math.max(0, delta))
                .build());
      }
    } catch (Exception ex) {
      log.warn(
          "upsertAndAdd 실패, 폴백 저장 시도 articleId={} delta={} err={}",
          articleId,
          delta,
          ex.getMessage());
      // 폴백: 존재하지 않으면 insert 시도, 존재하면 update
      ArticleCommentCount acc =
          repository
              .findById(articleId)
              .orElseGet(
                  () -> ArticleCommentCount.builder().articleId(articleId).commentCount(0).build());
      acc.setCount(Math.max(0, acc.getCommentCount() + delta));
      repository.save(acc);
    }
  }

  @Override
  @Transactional
  public void increment(String articleId) {
    int updated = repository.incrementCommentCount(articleId);
    if (updated == 0) {
      // 레코드가 없을 경우 생성 (delta = 1)
      repository.upsertAndAdd(articleId, 1);
    }
  }

  @Override
  @Transactional
  public void decrement(String articleId) {
    int updated = repository.decrementCommentCount(articleId);
    if (updated == 0) {
      // 만약 레코드가 없다면 생성할 필요 없음(기본 0). 하지만 안전하게 upsert로 보정 가능
      repository.upsertAndAdd(articleId, 0);
    }
  }

  @Override
  @Transactional
  public void setCount(String articleId, int count) {
    // 존재하지 않으면 save, 존재하면 update
    int updated = repository.setCount(articleId, Math.max(0, count));
    if (updated == 0) {
      ArticleCommentCount acc =
          ArticleCommentCount.builder()
              .articleId(articleId)
              .commentCount(Math.max(0, count))
              .build();
      repository.save(acc);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Integer> getCountsForArticles(List<String> articleIds) {
    // 조회 결과를 유지된 순서로 반환하기 위해 LinkedHashMap 사용
    Map<String, Integer> result = new LinkedHashMap<>();
    if (articleIds == null || articleIds.isEmpty()) {
      return result;
    }

    // repository.findAllById 사용: 반환은 Iterable<ArticleCommentCount>
    Iterable<ArticleCommentCount> iterable = repository.findAllById(articleIds);
    List<ArticleCommentCount> rows = StreamSupport.stream(iterable.spliterator(), false).toList();

    // Map으로 빠르게 조회할 수 있게 변환
    Map<String, Integer> existing =
        rows.stream()
            .collect(
                Collectors.toMap(
                    ArticleCommentCount::getArticleId, ArticleCommentCount::getCommentCount));

    // 요청된 articleIds 순서대로 채움. 없는 항목은 0으로 채움.
    for (String id : articleIds) {
      result.put(id, existing.getOrDefault(id, 0));
    }

    return result;
  }
}
