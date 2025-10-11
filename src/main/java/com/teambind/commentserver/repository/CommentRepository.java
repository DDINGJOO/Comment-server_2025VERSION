package com.teambind.commentserver.repository;

import com.teambind.commentserver.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

  List<Comment> findByParentCommentIdOrderByCreatedAtAsc(String parentCommentId);

  List<Comment> findByArticleIdOrderByCreatedAtAsc(String articleId);

  List<Comment> findByRootCommentIdOrderByCreatedAtAsc(String rootCommentId);
}
