package com.spn.viger_app.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho Chuyên mục (Category) của bài viết blog.
 * Ví dụ chuyên mục: "Học phí Đức", "Cuộc sống Đức", "Du học Đức"...
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tên chuyên mục, ví dụ: "Học phí Đức"
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Đường dẫn tĩnh (slug) của chuyên mục, ví dụ: "hoc-phi-duc"
     * Phải là duy nhất trong toàn bộ hệ thống.
     */
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    /**
     * Danh sách các bài viết thuộc chuyên mục này.
     * mappedBy trỏ đến trường "category" trong entity Post.
     */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();

    public Category() {
    }

    public Category(Long id, String name, String slug, List<Post> posts) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.posts = posts;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public static CategoryBuilder builder() {
        return new CategoryBuilder();
    }

    public static class CategoryBuilder {
        private Long id;
        private String name;
        private String slug;
        private List<Post> posts;

        CategoryBuilder() {
        }

        public CategoryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public CategoryBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CategoryBuilder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public CategoryBuilder posts(List<Post> posts) {
            this.posts = posts;
            return this;
        }

        public Category build() {
            return new Category(id, name, slug, posts);
        }
    }
}
