package com.spn.viger_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho Bài viết (Post) trên blog.
 * Mỗi bài viết thuộc một Chuyên mục (Category) — quan hệ @ManyToOne.
 */
@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
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

    /**
     * Tự động gán thời gian tạo trước khi lưu vào DB lần đầu.
     */
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
