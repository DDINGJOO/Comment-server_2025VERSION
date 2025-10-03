package com.teambind.commentserver.repository;

import com.teambind.commentserver.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {
  List<Comment> findByArticleIdAndIsDeletedFalseOrderByCreatedAtAsc(String articleId);

  List<Comment> findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(String parentCommentId);

  List<Comment> findByRootCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(String rootCommentId);
}
