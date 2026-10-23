package com.spn.viger_app.repository;

import java.util.List;
import java.util.Optional;

import com.spn.viger_app.entity.Post;
import com.spn.viger_app.entity.PostStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.spn.viger_app.entity.Category;


@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Post> findByCategory(Category category);
    List<Post> findTop5ByOrderByCreatedAtDesc();
    Optional<Post> findFirstByStatusOrderByCreatedAtDesc(PostStatus status);

    @Query("""
        select p from Post p
        where p.status = :status
        and (:categoryId is null or p.category.id = :categoryId)
        and p.title like concat('%', :q, '%')
        and (:excludeId is null or p.id <> :excludeId)
        """)
    Page<Post> searchPosts(@Param("status") PostStatus status,
                        @Param("categoryId") Long categoryId,
                        @Param("q") String q,
                        @Param("excludeId") Long excludeId,
                        Pageable pageable);
}
