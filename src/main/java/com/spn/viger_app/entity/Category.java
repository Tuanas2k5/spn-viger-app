package com.spn.viger_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Entity đại diện cho Chuyên mục (Category) của bài viết blog.
 * Ví dụ chuyên mục: "Học phí Đức", "Cuộc sống Đức", "Du học Đức"...
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private List<Post> posts;
}
