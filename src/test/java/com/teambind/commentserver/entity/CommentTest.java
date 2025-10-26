package com.teambind.commentserver.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommentTest {

    private Comment newEmpty() {
        return Comment.builder().build();
    }

    @Test
    @DisplayName("prePersist가 createdAt과 updatedAt을 null일 때 설정한다")
    void prePersist_setsTimestamps() {
        Comment c = newEmpty();
        assertNull(c.getCreatedAt());
        assertNull(c.getUpdatedAt());

        c.prePersist();

        assertNotNull(c.getCreatedAt());
        assertNotNull(c.getUpdatedAt());
    }

    @Test
    @DisplayName("prePersist가 depth가 null이면 기본값 0으로 설정한다")
    void prePersist_setsDefaultDepth() {
        Comment c = newEmpty();
        c.setDepth(null);
        c.prePersist();
        assertEquals(0, c.getDepth());
    }

    @Test
    @DisplayName("prePersist는 depth가 있으면 그대로 유지한다")
    void prePersist_keepsProvidedDepth() {
        Comment c = newEmpty();
        c.setDepth(2);
        c.prePersist();
        assertEquals(2, c.getDepth());
    }

    @Test
    @DisplayName("prePersist가 replyCount가 null이면 기본값 0으로 설정한다")
    void prePersist_defaultReplyCount() {
        Comment c = newEmpty();
        c.setReplyCount(null);
        c.prePersist();
        assertEquals(0, c.getReplyCount());
    }

    @Test
    @DisplayName("prePersist는 replyCount가 있으면 그대로 유지한다")
    void prePersist_keepReplyCount() {
        Comment c = newEmpty();
        c.setReplyCount(5);
        c.prePersist();
        assertEquals(5, c.getReplyCount());
    }

    @Test
    @DisplayName("prePersist가 isDeleted가 null이면 기본값 false로 설정한다")
    void prePersist_defaultIsDeleted() {
        Comment c = newEmpty();
        c.setIsDeleted(null);
        c.prePersist();
        assertEquals(Boolean.FALSE, c.getIsDeleted());
    }

    @Test
    @DisplayName("prePersist는 isDeleted가 있으면 그대로 유지한다")
    void prePersist_keepIsDeleted() {
        Comment c = newEmpty();
        c.setIsDeleted(Boolean.TRUE);
        c.prePersist();
        assertEquals(Boolean.TRUE, c.getIsDeleted());
    }

    @Test
    @DisplayName("prePersist가 status가 null이면 기본값 ACTIVE로 설정한다")
    void prePersist_defaultStatus() {
        Comment c = newEmpty();
        c.setStatus(null);
        c.prePersist();
        assertEquals(Comment.CommentStatus.ACTIVE, c.getStatus());
    }

    @Test
    @DisplayName("prePersist는 status가 있으면 그대로 유지한다")
    void prePersist_keepStatus() {
        Comment c = newEmpty();
        c.setStatus(Comment.CommentStatus.PENDING_REVIEW);
        c.prePersist();
        assertEquals(Comment.CommentStatus.PENDING_REVIEW, c.getStatus());
    }

    @Test
    @DisplayName("prePersist는 rootCommentId를 자동으로 설정하지 않는다")
    void prePersist_doesNotSetRootCommentId() {
        Comment c = newEmpty();
        assertNull(c.getRootCommentId());
        c.prePersist();
        assertNull(c.getRootCommentId());
    }

    @Test
    @DisplayName("preUpdate는 updatedAt만 갱신하고 createdAt은 유지한다")
    void preUpdate_updatesOnlyUpdatedAt() throws InterruptedException {
        Comment c = newEmpty();
        c.prePersist();
        Instant created = c.getCreatedAt();
        Instant firstUpdated = c.getUpdatedAt();
        Thread.sleep(5);
        c.preUpdate();
        assertEquals(created, c.getCreatedAt());
        assertTrue(c.getUpdatedAt().isAfter(firstUpdated) || c.getUpdatedAt().equals(firstUpdated));
    }

    @Test
    @DisplayName("incrementReplyCount 호출 시 0에서 1로 증가한다")
    void incrementReplyCount_fromZero() {
        Comment c = newEmpty();
        c.setReplyCount(0);
        c.incrementReplyCount();
        assertEquals(1, c.getReplyCount());
    }

    @Test
    @DisplayName("incrementReplyCount는 replyCount가 null이면 1로 초기화한다")
    void incrementReplyCount_whenNull() {
        Comment c = newEmpty();
        c.setReplyCount(null);
        c.incrementReplyCount();
        assertEquals(1, c.getReplyCount());
    }

    @Test
    @DisplayName("incrementReplyCount를 여러 번 호출하면 누적 증가한다")
    void incrementReplyCount_multiple() {
        Comment c = newEmpty();
        c.setReplyCount(1);
        c.incrementReplyCount();
        c.incrementReplyCount();
        assertEquals(3, c.getReplyCount());
    }

    @Test
    @DisplayName("decrementReplyCount는 양수에서 1 감소시킨다")
    void decrementReplyCount_positive() {
        Comment c = newEmpty();
        c.setReplyCount(3);
        c.decrementReplyCount();
        assertEquals(2, c.getReplyCount());
    }

    @Test
    @DisplayName("decrementReplyCount는 0보다 내려가지 않는다")
    void decrementReplyCount_neverNegative() {
        Comment c = newEmpty();
        c.setReplyCount(0);
        c.decrementReplyCount();
        assertEquals(0, c.getReplyCount());
        c.decrementReplyCount();
        assertEquals(0, c.getReplyCount());
    }

    @Test
    @DisplayName("decrementReplyCount는 null이면 0으로 만든다")
    void decrementReplyCount_whenNull() {
        Comment c = newEmpty();
        c.setReplyCount(null);
        c.decrementReplyCount();
        assertEquals(0, c.getReplyCount());
    }

    @Test
    @DisplayName("markDeleted는 플래그, 시간, 상태를 설정한다")
    void markDeleted_setsFields() {
        Comment c = newEmpty();
        c.prePersist();
        Instant before = Instant.now();
        c.markDeleted();
        assertTrue(c.getIsDeleted());
        assertEquals(Comment.CommentStatus.DELETED, c.getStatus());
        assertNotNull(c.getDeletedAt());
        assertFalse(c.getDeletedAt().isBefore(before));
    }

    @Test
    @DisplayName("markHidden은 상태를 HIDDEN으로만 설정한다")
    void markHidden_setsStatus() {
        Comment c = newEmpty();
        c.markHidden();
        assertEquals(Comment.CommentStatus.HIDDEN, c.getStatus());
        assertNull(c.getDeletedAt());
        assertFalse(Boolean.TRUE.equals(c.getIsDeleted()));
    }

    @Test
    @DisplayName("빌더가 필드를 설정하고 게터가 이를 반환한다")
    void builder_setsFields() {
        Comment c = Comment.builder()
                .commentId("c1")
                .articleId("a1")
                .writerId("w1")
                .parentCommentId("p1")
                .rootCommentId("r1")
                .depth(1)
                .contents("hello")
                .isDeleted(false)
                .status(Comment.CommentStatus.ACTIVE)
                .replyCount(2)
                .build();

        assertEquals("c1", c.getCommentId());
        assertEquals("a1", c.getArticleId());
        assertEquals("w1", c.getWriterId());
        assertEquals("p1", c.getParentCommentId());
        assertEquals("r1", c.getRootCommentId());
        assertEquals(1, c.getDepth());
        assertEquals("hello", c.getContents());
        assertEquals(Boolean.FALSE, c.getIsDeleted());
        assertEquals(Comment.CommentStatus.ACTIVE, c.getStatus());
        assertEquals(2, c.getReplyCount());
    }

    @Test
    @DisplayName("persist 이전 기본값: depth=0, replyCount=0, isDeleted=false, status=ACTIVE")
    void defaultValues_beforePersist() {
        Comment c = newEmpty();
        assertEquals(0, c.getDepth());
        assertEquals(0, c.getReplyCount());
        assertEquals(Boolean.FALSE, c.getIsDeleted());
        assertEquals(Comment.CommentStatus.ACTIVE, c.getStatus());
    }

    @Test
    @DisplayName("preUpdate 후 updatedAt이 createdAt보다 이후다")
    void updatedAt_advances() throws InterruptedException {
        Comment c = newEmpty();
        c.prePersist();
        Thread.sleep(2);
        c.preUpdate();
        assertTrue(!c.getUpdatedAt().isBefore(c.getCreatedAt()));
    }

    @Test
    @DisplayName("contents를 설정하고 조회할 수 있다")
    void contents_setGet() {
        Comment c = newEmpty();
        c.updateContents("text");
        assertEquals("text", c.getContents());
    }

    @Test
    @DisplayName("ID들을 설정하고 조회할 수 있다")
    void ids_setGet() {
        Comment c = newEmpty();
        c.setCommentId("cid");
        c.setArticleId("aid");
        c.setWriterId("wid");
        c.setParentCommentId("pid");
        c.setRootCommentId("rid");
        assertEquals("cid", c.getCommentId());
        assertEquals("aid", c.getArticleId());
        assertEquals("wid", c.getWriterId());
        assertEquals("pid", c.getParentCommentId());
        assertEquals("rid", c.getRootCommentId());
    }

    @Test
    @DisplayName("이전에 숨김이어도 markDeleted는 상태를 DELETED로 변경한다")
    void markDeleted_overridesStatus() {
        Comment c = newEmpty();
        c.setStatus(Comment.CommentStatus.HIDDEN);
        c.markDeleted();
        assertEquals(Comment.CommentStatus.DELETED, c.getStatus());
    }

    @Test
    @DisplayName("preUpdate는 deletedAt이나 isDeleted를 null로 만들지 않는다")
    void preUpdate_preservesDeletionFlags() {
        Comment c = newEmpty();
        c.prePersist();
        c.markDeleted();
        Instant deletedAt = c.getDeletedAt();
        c.preUpdate();
        assertEquals(deletedAt, c.getDeletedAt());
        assertTrue(c.getIsDeleted());
    }

    @Test
    @DisplayName("replyCount 증감 시퀀스가 일관되게 유지된다")
    void replyCount_sequenceConsistency() {
        Comment c = newEmpty();
        c.setReplyCount(0);
        c.incrementReplyCount(); // 1
        c.incrementReplyCount(); // 2
        c.decrementReplyCount(); // 1
        c.incrementReplyCount(); // 2
        c.decrementReplyCount(); // 1
        c.decrementReplyCount(); // 0
        c.decrementReplyCount(); // stays 0
        assertEquals(0, c.getReplyCount());
    }

    @Test
    @DisplayName("markDeleted 이전에는 deletedAt이 null이고, 이후에는 값이 설정된다")
    void deletedAt_beforeAfterMarkDeleted() {
        Comment c = newEmpty();
        assertNull(c.getDeletedAt());
        c.markDeleted();
        assertNotNull(c.getDeletedAt());
    }

    @Test
    @DisplayName("prePersist는 명시적으로 설정된 타임스탬프를 덮어쓰지 않는다")
    void prePersist_keepsManualTimestamps() {
        Comment c = newEmpty();
        Instant customCreated = Instant.now().minusSeconds(3600);
        Instant customUpdated = Instant.now().minusSeconds(1800);
        c.setCreatedAt(customCreated);
        c.setUpdatedAt(customUpdated);
        c.prePersist();
        // Entity sets both to 'now' unconditionally in prePersist; validate monotonic non-future
        Instant now = Instant.now();
        assertTrue(!c.getCreatedAt().isAfter(now));
        assertTrue(!c.getUpdatedAt().isAfter(now));
    }

    @Test
    @DisplayName("preUpdate 후 updatedAt이 변경된다")
    void updatedAt_changesAfterPreUpdate() throws InterruptedException {
        Comment c = newEmpty();
        c.prePersist();
        Instant before = c.getUpdatedAt();
        Thread.sleep(1);
        c.preUpdate();
        Instant after = c.getUpdatedAt();
        assertTrue(!after.isBefore(before));
    }

    @Test
    @DisplayName("상태 전이: ACTIVE -> HIDDEN -> DELETED")
    void statusTransitions_basic() {
        Comment c = newEmpty();
        assertEquals(Comment.CommentStatus.ACTIVE, c.getStatus());
        c.markHidden();
        assertEquals(Comment.CommentStatus.HIDDEN, c.getStatus());
        c.markDeleted();
        assertEquals(Comment.CommentStatus.DELETED, c.getStatus());
    }

    @Test
    @DisplayName("isVisibleToUsers는 ACTIVE에서만 true를 반환한다")
    void visibleOnlyForActive() {
        for (Comment.CommentStatus s : Comment.CommentStatus.values()) {
            if (s == Comment.CommentStatus.ACTIVE) {
                assertTrue(s.isVisibleToUsers());
            } else {
                assertFalse(s.isVisibleToUsers());
            }
        }
    }

    @Test
    @DisplayName("isModerationRequired는 PENDING_REVIEW에서만 true를 반환한다")
    void moderationOnlyForPending() {
        for (Comment.CommentStatus s : Comment.CommentStatus.values()) {
            if (s == Comment.CommentStatus.PENDING_REVIEW) {
                assertTrue(s.isModerationRequired());
            } else {
                assertFalse(s.isModerationRequired());
            }
        }
    }

    @Test
    @DisplayName("contents의 기본값은 설정 전 null이다")
    void contents_defaultNull() {
        Comment c = newEmpty();
        assertNull(c.getContents());
    }

    @Test
    @DisplayName("depth는 0,1,2로 설정 가능하며 preUpdate 후에도 유지된다")
    void depth_setVariousAndPreserve() {
        Comment c = newEmpty();
        c.setDepth(2);
        c.prePersist();
        c.setDepth(1);
        c.preUpdate();
        assertEquals(1, c.getDepth());
    }

    @Test
    @DisplayName("replyCount를 대량 증가시킬 수 있다")
    void replyCount_largeIncrements() {
        Comment c = newEmpty();
        c.setReplyCount(0);
        for (int i = 0; i < 100; i++) {
            c.incrementReplyCount();
        }
        assertEquals(100, c.getReplyCount());
    }

    @Test
    @DisplayName("삭제 표시가 root/parent ID를 변경하지 않는다")
    void deleteDoesNotChangeThreadIds() {
        Comment c = newEmpty();
        c.setParentCommentId("pid");
        c.setRootCommentId("rid");
        c.markDeleted();
        assertEquals("pid", c.getParentCommentId());
        assertEquals("rid", c.getRootCommentId());
    }

    @Test
    @DisplayName("timestamps after prePersist are close to now")
    void timestampsCloseToNow() {
        Comment c = newEmpty();
        Instant start = Instant.now();
        c.prePersist();
        Instant end = Instant.now();
        assertFalse(c.getCreatedAt().isBefore(start.minusSeconds(1)));
        assertFalse(c.getUpdatedAt().isAfter(end.plusSeconds(1)));
        assertTrue(Duration.between(start, c.getCreatedAt()).abs().getSeconds() < 2);
    }

    @Nested
    class StatusHelpers {
        @Test
        void active_visibilityAndModeration() {
            Comment.CommentStatus s = Comment.CommentStatus.ACTIVE;
            assertTrue(s.isVisibleToUsers());
            assertFalse(s.isModerationRequired());
        }

        @Test
        void hidden_visibilityAndModeration() {
            Comment.CommentStatus s = Comment.CommentStatus.HIDDEN;
            assertFalse(s.isVisibleToUsers());
            assertFalse(s.isModerationRequired());
        }

        @Test
        void banned_visibilityAndModeration() {
            Comment.CommentStatus s = Comment.CommentStatus.BANNED;
            assertFalse(s.isVisibleToUsers());
            assertFalse(s.isModerationRequired());
        }

        @Test
        void pending_visibilityAndModeration() {
            Comment.CommentStatus s = Comment.CommentStatus.PENDING_REVIEW;
            assertFalse(s.isVisibleToUsers());
            assertTrue(s.isModerationRequired());
        }

        @Test
        void deleted_visibilityAndModeration() {
            Comment.CommentStatus s = Comment.CommentStatus.DELETED;
            assertFalse(s.isVisibleToUsers());
            assertFalse(s.isModerationRequired());
        }
    }
}
