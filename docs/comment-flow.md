# 댓글 생성/삭제 흐름과 이벤트 발행 (요약)

본 문서는 Comment-server에서 댓글 생성/삭제 시 내부 처리 순서와 이벤트 발행 기준을 요약합니다.

## 1. 댓글 생성 (루트/답글 공통)

1) 댓글 엔티티 저장 (JPA)
	- 루트 댓글: `depth=0`, `rootCommentId=self`
	- 답글: 부모의 `articleId`를 상속, `depth=parent.depth+1`
2) ArticleCommentCount 증가
	- `articleCommentCountService.increment(articleId)` 호출로 아티클별 총 댓글 수(+1)
3) "첫 댓글" 이벤트 발행 여부 판단 (Redis)
	- 키: `c:first:v1:{articleId}:{writerId}`
	- 연산: `SETNX` + TTL 2일
	- 성공 시(`true`)에만 `comment-created` 이벤트 발행
	- 이미 키가 있으면(2일 내 재시도/중복) 이벤트 스킵
	- Redis 장애 시: 안전을 위해 스킵(필요 시 DB 폴백 확장 가능)

## 2. 댓글 삭제 (소프트 삭제)

1) 권한 및 존재 검증 후 댓글 소프트 삭제(`isDeleted=true`, `status=DELETED`)
2) "마지막 댓글" 이벤트 발행 여부 판단 (DB 조회)
	- 조건: 해당 아티클의 활성 댓글 수(`isDeleted=false AND status=ACTIVE`)가 0이면 발행
	- 구현: `commentRepository.countByArticleIdAndIsDeletedFalseAndStatus(articleId, ACTIVE)`
	- 0인 경우 `comment-deleted` 이벤트 발행
	- 요청에 따라 삭제 시 카운터 테이블 기반이 아닌 DB COUNT 사용을 유지

## 3. 이벤트 발행

- 퍼블리셔: `EventPublisher`
- 토픽:
	- 첫 댓글: `comment-created`
	- 마지막 댓글: `comment-deleted`
- 페이로드: `writerId`, `articleId`, `createdAt`

## 4. 설계 의도

- 첫 댓글 판단은 버스트 트래픽에서 DB 부하를 줄이기 위해 Redis(2일 TTL) 사용
	- 커뮤니티 특성상 단기간 반복 액션을 흡수하기 위함
	- downstream(프로필 서버)에서 아티클ID 중복 저장을 방어하므로 TTL 만료 이후 재발행 영향은 제한적
- 마지막 댓글은 정확성이 중요하므로 DB 기준으로 판단

## 5. 운영 주의사항

- Redis 키 TTL: 2일(`c:first:v1:*`)
- 모니터링: Redis 연결 오류율, Kafka 발행 실패율
- 장애 시 정책: 첫 댓글 이벤트는 보수적으로 스킵(폴백 전략 필요 시 별도 플래그로 제어)

