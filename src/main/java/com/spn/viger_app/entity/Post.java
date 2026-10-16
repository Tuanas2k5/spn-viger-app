package com.spn.viger_app.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho Bài viết (Post) trên blog.
 * Mỗi bài viết thuộc một Chuyên mục (Category) — quan hệ @ManyToOne.
 */
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tiêu đề bài viết
     */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * Nội dung đầy đủ của bài viết (lưu dạng LONGTEXT)
     */
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    /**
     * Đoạn tóm tắt ngắn gọn (excerpt / mô tả ngắn)
     */
    @Column(name = "excerpt", length = 1000)
    private String excerpt;

    /**
     * Ngày tạo bài viết
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Đường dẫn tĩnh (slug) dùng cho URL thân thiện.
     * Ví dụ: "du-hoc-duc" → URL: /bai-viet/du-hoc-duc
     * Phải là duy nhất trong toàn bộ hệ thống.
     */
    @Column(name = "slug", nullable = false, unique = true, length = 500)
    private String slug;

    /**
     * Trạng thái bài viết: PUBLISHED (Xuất bản) hoặc DRAFT (Nháp)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PostStatus status = PostStatus.DRAFT;

    /**
     * Đường dẫn / URL ảnh đại diện bài viết (featured image)
     */
    @Column(name = "featured_image", length = 1000)
    private String featuredImage;

    /**
     * Chuyên mục của bài viết (quan hệ Many-to-One với Category).
     * Nhiều bài viết có thể thuộc cùng một chuyên mục.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    public Post() {
    }

    public Post(Long id, String title, String content, String excerpt, LocalDateTime createdAt,
                String slug, PostStatus status, String featuredImage, Category category) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.excerpt = excerpt;
        this.createdAt = createdAt;
        this.slug = slug;
        this.status = status != null ? status : PostStatus.DRAFT;
        this.featuredImage = featuredImage;
        this.category = category;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    public String getFeaturedImage() {
        return featuredImage;
    }

    public void setFeaturedImage(String featuredImage) {
        this.featuredImage = featuredImage;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public static PostBuilder builder() {
        return new PostBuilder();
    }

    public static class PostBuilder {
        private Long id;
        private String title;
        private String content;
        private String excerpt;
        private LocalDateTime createdAt;
        private String slug;
        private PostStatus status = PostStatus.DRAFT;
        private String featuredImage;
        private Category category;

        PostBuilder() {
        }

        public PostBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PostBuilder title(String title) {
            this.title = title;
            return this;
        }

        public PostBuilder content(String content) {
            this.content = content;
            return this;
        }

        public PostBuilder excerpt(String excerpt) {
            this.excerpt = excerpt;
            return this;
        }

        public PostBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PostBuilder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public PostBuilder status(PostStatus status) {
            this.status = status;
            return this;
        }

        public PostBuilder featuredImage(String featuredImage) {
            this.featuredImage = featuredImage;
            return this;
        }

        public PostBuilder category(Category category) {
            this.category = category;
            return this;
        }

        public Post build() {
            return new Post(id, title, content, excerpt, createdAt, slug, status, featuredImage, category);
        }
    }
}
