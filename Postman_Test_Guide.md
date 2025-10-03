# 댓글 서비스 Postman 테스트 가이드

이 문서는 Postman으로 댓글 API를 빠르게 테스트할 수 있도록 URL과 요청 바디 예시를 제공합니다.

- Base URL (로컬): http://localhost:8080
- 공통 Prefix: /api/comments
- 프로필: 기본은 dev (루트의 .env의 SPRING_PROFILES_ACTIVE=dev)
- 에러 응답: 게이트웨이로 전달될 수 있도록 문자열(String) 본문으로 반환됩니다. (예: "댓글을 찾을 수 없습니다.")

참고: DB/H2 설정 등은 테스트 프로필에서 자동 구성되며, 실제 로컬 실행 시에는 application-dev.yaml과 .env 설정을 참고하세요.

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
  "isDeleted": false,
  "status": "ACTIVE",
  "replyCount": 0,
  "createdAt": "2025-10-03T10:23:45Z",
  "updatedAt": "2025-10-03T10:23:45Z",
  "deletedAt": null
  }
- 유효성 오류 (400, String)
  "writerId는 필수입니다."

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
- 성공 응답 (201 Created, JSON) — 루트 댓글과 구조 동일, parentCommentId/rootCommentId/depth만 달라짐
- 부모 없음 오류 (404, String)
  "부모 댓글을 찾을 수 없습니다."

---

## 3) 특정 아티클의 전체 댓글 조회 (삭제되지 않은 것만)

- Method: GET
- URL: {{baseUrl}}/api/comments/article/{articleId}
- Path Variable: articleId
- 성공 응답 (200 OK, JSON Array)
  [
  {
  "commentId": "...",
  "articleId": "{articleId}",
  "writerId": "user-1",
  "parentCommentId": null,
  "rootCommentId": "...",
  "depth": 0,
  "contents": "내용",
  "isDeleted": false,
  "status": "ACTIVE",
  "replyCount": 0,
  "createdAt": "...",
  "updatedAt": "...",
  "deletedAt": null
  }
  ]

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

- 내용 수정
  curl -X PATCH "http://localhost:8080/api/comments/{id}" \
  -H "Content-Type: application/json" \
  -d '{
  "writerId": "user-1",
  "contents": "수정된 내용"
  }'

- 소프트 삭제
  curl -X DELETE "http://localhost:8080/api/comments/{id}?writerId=user-1"
