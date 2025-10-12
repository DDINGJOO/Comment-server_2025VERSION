# 댓글 서비스 Postman 테스트 가이드 (업데이트)

이 문서는 최신 API 및 DTO 변경사항을 반영하여 Postman으로 댓글 API를 테스트할 수 있도록 URL과 요청/응답 예시를 제공합니다.

- Base URL (로컬): http://localhost:8080
- 공통 Prefix: /api/comments
- 프로필: 기본은 dev (루트의 .env의 SPRING_PROFILES_ACTIVE=dev)
- 에러 응답: 게이트웨이로 전달될 수 있도록 문자열(String) 본문으로 반환됩니다. (예: "댓글을 찾을 수 없습니다.")
- 참고: DB/H2 설정 등은 테스트 프로필에서 자동 구성되며, 실제 로컬 실행 시에는 application-dev.yaml과 .env 설정을 참고하세요.

변경 핵심 요약

- 응답 DTO(CommentResponse) 변경: isDeleted, status, updatedAt, deletedAt 필드가 응답에서 제외되었습니다. 대신 replies[], isEdited, visible
  필드가 추가되었습니다.
- 목록 조회 API(/article/{articleId})가 페이지네이션 쿼리 파라미터(page, pageSize)와 mode=visibleCount|all을 지원합니다. 기본은 visibleCount 방식입니다.
- 콘텐츠 마스킹 규칙: 삭제/숨김/제재 상태일 때 contents와 visible 플래그가 정책에 맞게 변환되어 내려갑니다.

---

## 1) 루트 댓글 생성

- Method: POST
- URL: {{baseUrl}}/api/comments
- Headers: Content-Type: application/json
- Request Body (JSON 예시)
  {
  "articleId": "article-1",
  "writerId": "user-1",
  "contents": "첫 댓글입니다."
  }
- 성공 응답 (201 Created, JSON)
  {
  "commentId": "generated-id",
  "articleId": "article-1",
  "writerId": "user-1",
  "parentCommentId": null,
  "rootCommentId": "generated-id",
  "depth": 0,
  "contents": "첫 댓글입니다.",
  "replyCount": 0,
  "createdAt": "2025-10-03T10:23:45Z",
  "replies": [],
  "isEdited": false,
  "visible": true
  }
- 유효성 오류 (400, String)
  "writerId는 필수입니다."

설명

- isEdited: createdAt과 updatedAt이 다르면 true로 내려갑니다.
- visible: HIDDEN/BANNED/PENDING_REVIEW 등일 때 false가 될 수 있습니다. 삭제된 댓글은 트리 보존을 위해 visible=true이며 contents가 "삭제된 댓글입니다."로
  대체됩니다.

---

## 2) 대댓글(자식 댓글) 생성

- Method: POST
- URL: {{baseUrl}}/api/comments/{parentId}/replies
- Path Variable: parentId = 부모 댓글의 commentId
- Headers: Content-Type: application/json
- Request Body (JSON 예시)
  {
  "writerId": "user-2",
  "contents": "부모 댓글에 대한 대댓글입니다."
  }
- 성공 응답 (201 Created, JSON)
	- 루트 댓글과 구조 동일하며 parentCommentId/rootCommentId/depth가 설정됩니다.
- 부모 없음 오류 (404, String)
  "부모 댓글을 찾을 수 없습니다."

---

## 3) 특정 아티클의 댓글 목록 조회 (삭제/제재 정책 반영)

- Method: GET
- URL: {{baseUrl}}/api/comments/article/{articleId}
- Path Variable: articleId
- Query Params (선택)
	- page: 기본 0
	- pageSize: 기본 10
	- mode: visibleCount | all (기본 visibleCount)
		- visibleCount: 루트 단위로 화면에 보이는 개수(루트+자식 합)를 기준으로 페이징됨.
		- all: 기존 방식으로 해당 아티클의 모든 댓글을 조회하여 반환.
- 성공 응답 (200 OK, JSON Array)
  [
  {
  "commentId": "...",
  "articleId": "{articleId}",
  "writerId": "user-1",
  "parentCommentId": null,
  "rootCommentId": "...",
  "depth": 0,
  "contents": "내용 또는 정책에 따른 대체 문자열",
  "replyCount": 0,
  "createdAt": "...",
  "replies": [],
  "isEdited": false,
  "visible": true
  }
  ]

콘텐츠 마스킹 규칙 요약

- isDeleted=true 또는 status=DELETED: contents = "삭제된 댓글입니다.", visible = true
- status=HIDDEN: contents = "숨김 처리된 댓글입니다.", visible = false
- status=BANNED: contents = "제재된 댓글입니다.", visible = false
- status=PENDING_REVIEW: contents = "검토 중인 댓글입니다.", visible = false

---

## 4) 특정 부모의 대댓글 목록 조회

- Method: GET
- URL: {{baseUrl}}/api/comments/{parentId}/replies
- Path Variable: parentId
- 성공 응답 (200 OK, JSON Array)
  [ { 댓글 JSON ... }, { 댓글 JSON ... } ]

---

## 5) 루트 댓글 기준 스레드 전체 조회

- Method: GET
- URL: {{baseUrl}}/api/comments/thread/{rootId}
- Path Variable: rootId = 스레드 루트(comment.rootCommentId)
- 성공 응답 (200 OK, JSON Array)
  [ { 댓글 JSON ... }, { 댓글 JSON ... } ]

---

## 6) 단건 조회

- Method: GET
- URL: {{baseUrl}}/api/comments/{id}
- Path Variable: id = commentId
- 성공 응답 (200 OK, JSON)
  { 댓글 JSON ... }
- 미존재 (404, String)
  "댓글을 찾을 수 없습니다."

---

## 7) 댓글 내용 수정 (작성자 본인만)

- Method: PATCH
- URL: {{baseUrl}}/api/comments/{id}
- Path Variable: id = commentId
- Headers: Content-Type: application/json
- Request Body (JSON 예시)
  {
  "writerId": "user-1",
  "contents": "수정한 내용"
  }
- 성공 응답 (200 OK, JSON)
  { 수정된 댓글 JSON ... }
- 본인 아님 (403, String)
  "작성자 본인만 댓글을 수정/삭제할 수 있습니다."
- 내용 비어있음 (400, String)
  "댓글 내용은 비어 있을 수 없습니다."
- 미존재 (404, String)
  "댓글을 찾을 수 없습니다."

---

## 8) 소프트 삭제 (작성자 본인만)

- Method: DELETE
- URL: {{baseUrl}}/api/comments/{id}?writerId={writerId}
- Path Variables/Query: id = commentId, writerId = 요청자 ID
- 성공 응답 (204 No Content)
- 본인 아님 (403, String)
  "작성자 본인만 댓글을 수정/삭제할 수 있습니다."
- 미존재 (404, String)
  "댓글을 찾을 수 없습니다."

---

## Postman 환경 설정 팁

- 환경 변수 설정 예시
	- baseUrl = http://localhost:8080
- 컬렉션 순서 제안
	1) 루트 댓글 생성 → 응답의 commentId 저장(rootId)
	2) 대댓글 생성(parentId = rootId)
	3) 단건 조회(id = rootId)
	4) 스레드 조회(rootId)
	5) 아티클 전체 조회(articleId)
	6) 내용 수정(id = rootId, writerId = 작성자)
	7) 소프트 삭제(id = rootId, writerId = 작성자)

---

## cURL 예시 모음

- 루트 댓글 생성
  curl -X POST "http://localhost:8080/api/comments" \
  -H "Content-Type: application/json" \
  -d '{
  "articleId": "article-1",
  "writerId": "user-1",
  "contents": "첫 댓글입니다."
  }'

- 대댓글 생성
  curl -X POST "http://localhost:8080/api/comments/{parentId}/replies" \
  -H "Content-Type: application/json" \
  -d '{
  "writerId": "user-2",
  "contents": "대댓글입니다."
  }'

- 아티클 댓글 목록 조회 (visibleCount 페이징)
  curl -G "http://localhost:8080/api/comments/article/{articleId}" \
  --data-urlencode "page=0" \
  --data-urlencode "pageSize=10" \
  --data-urlencode "mode=visibleCount"

- 아티클 댓글 목록 조회 (all 모드)
  curl -G "http://localhost:8080/api/comments/article/{articleId}" \
  --data-urlencode "mode=all"

- 내용 수정
  curl -X PATCH "http://localhost:8080/api/comments/{id}" \
  -H "Content-Type: application/json" \
  -d '{
  "writerId": "user-1",
  "contents": "수정된 내용"
  }'

- 소프트 삭제
  curl -X DELETE "http://localhost:8080/api/comments/{id}?writerId=user-1"
