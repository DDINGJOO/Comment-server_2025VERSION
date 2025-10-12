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
	
	public static CommentResponse from(Comment c) {
		    boolean edited = c.getUpdatedAt() != null && !c.getUpdatedAt().equals(c.getCreatedAt());
		
				    // 우선 isDeleted가 true면 작성자(혹은 관리자가) 소프트 삭제한 상태: 내용은 고정 메시지로 대체
		if (Boolean.TRUE.equals(c.getIsDeleted())) {
			      return CommentResponse.builder()
					          .commentId(c.getCommentId())
					          .articleId(c.getArticleId())
					          .writerId(c.getWriterId())
					          .parentCommentId(c.getParentCommentId())
					          .rootCommentId(c.getRootCommentId())
					          .depth(c.getDepth())
					          .contents("삭제된 댓글입니다.")
					          .replyCount(c.getReplyCount())
					          .createdAt(c.getCreatedAt())
					          .replies(new ArrayList<>())
					          .isEdited(edited)
					          .visible(true) // 삭제된 댓글도 트리 구조상 노출 (요구사항)
					          .build();
			    }
		
				    // isDeleted가 아닌 경우 status에 따라 내용/표시 여부 결정
		    boolean visible = true;
		    String contents = c.getContents();
		
				    switch (c.getStatus()) {
					      case ACTIVE -> { // 그대로 노출
						      }
					      case HIDDEN -> { // 신고/검토로 숨김: 내용 대체, 프론트에 숨김 표기
						        contents = "숨김 처리된 댓글입니다.";
        visible = true;
						      }
					      case BANNED -> { // 정책 위반: 내용을 완전 가리고 visible=false
						        contents = "제재된 댓글입니다.";
        visible = true;
						      }
					      case PENDING_REVIEW -> { // 검토중: 노출 여부 정책에 따라 숨기거나 요약 노출 가능 (여기선 숨김)
						        contents = "검토 중인 댓글입니다.";
        visible = true;
						      }
					      case DELETED -> { // status가 DELETED인 경우 (isDeleted는 false면 드문 케이스) — treat as deleted
						        contents = "삭제된 댓글입니다.";
					      }
					      default -> {
						      }
					    }
				
				    return CommentResponse.builder()
				        .commentId(c.getCommentId())
				        .articleId(c.getArticleId())
				        .writerId(c.getWriterId())
				        .parentCommentId(c.getParentCommentId())
				        .rootCommentId(c.getRootCommentId())
				        .depth(c.getDepth())
				        .contents(contents)
				        .replyCount(c.getReplyCount())
				        .createdAt(c.getCreatedAt())
				        .replies(new ArrayList<>())
				        .isEdited(edited)
				        .visible(visible)
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
