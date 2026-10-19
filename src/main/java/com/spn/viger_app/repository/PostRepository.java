package com.spn.viger_app.repository;

import java.util.List;
import java.util.Optional;

import com.spn.viger_app.entity.Post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spn.viger_app.entity.Category;


@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Post> findByCategory(Category category);
}
