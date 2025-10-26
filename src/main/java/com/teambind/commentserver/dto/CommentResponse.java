package com.teambind.commentserver.dto;

import com.teambind.commentserver.entity.Comment;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CommentResponse {
	private String commentId;
	private String articleId;
	private String writerId;
	private String parentCommentId;
	private String rootCommentId;
	private Integer depth;
	private String contents;
	private Integer replyCount;
	private Instant createdAt;
	
	@Builder.Default
	private List<CommentResponse> replies = new ArrayList<>();
	
	@Builder.Default
	private Boolean isEdited = Boolean.FALSE;

	
	@Builder.Default
	private Boolean visible = Boolean.TRUE;

	/**
	 * Comment 엔티티로부터 CommentResponse DTO를 생성합니다.
	 *
	 * <p>비즈니스 로직(상태별 컨텐츠 변환, 수정 여부 판단)은 Comment 엔티티의 도메인 메서드에 위임하여
	 * DTO의 책임을 데이터 전송으로 제한합니다.
	 *
	 * @param c Comment 엔티티
	 * @return CommentResponse DTO
	 */
	public static CommentResponse from(Comment c) {
		return CommentResponse.builder()
				.commentId(c.getCommentId())
				.articleId(c.getArticleId())
				.writerId(c.getWriterId())
				.parentCommentId(c.getParentCommentId())
				.rootCommentId(c.getRootCommentId())
				.depth(c.getDepth())
				.contents(c.getDisplayContents()) // 도메인 로직 위임
				.replyCount(c.getReplyCount())
				.createdAt(c.getCreatedAt())
				.replies(new ArrayList<>())
				.isEdited(c.isEdited()) // 도메인 로직 위임
				.visible(c.isVisibleInTree()) // 도메인 로직 위임
				.build();
	}
	
	
	// 편의 alias: 기존 코드가 from(...)를 사용하지 않을 경우를 대비
	public static CommentResponse fromEntity(Comment c) {
		return from(c);
	}
	
	// replies에 자식 추가 (서비스에서 호출하여 트리 구성)
	public void addReply(CommentResponse reply) {
		if (reply == null) {
			return;
		}
		if (this.replies == null) {
			// 안전하게 초기화
			// replies 필드는 빌더로 기본 초기화되지만 방어코드 추가
			//noinspection AssignmentToCollectionOrArrayFieldFromParameter
			this.replies = new ArrayList<>();
		}
		this.replies.add(reply);
	}
}
