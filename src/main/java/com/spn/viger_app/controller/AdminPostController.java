package com.spn.viger_app.controller;

import com.spn.viger_app.dto.PostForm;
import com.spn.viger_app.entity.Category;
import com.spn.viger_app.entity.Post;
import com.spn.viger_app.entity.PostStatus;
import com.spn.viger_app.repository.CategoryRepository;
import com.spn.viger_app.repository.PostRepository;
import com.spn.viger_app.util.SlugUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.spn.viger_app.service.WordPressXmlImportService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/admin/posts")
public class AdminPostController {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final WordPressXmlImportService importService;

    public AdminPostController(PostRepository postRepository, 
                               CategoryRepository categoryRepository,
                               WordPressXmlImportService importService) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.importService = importService;
    }

    @GetMapping
    public String listPosts(Model model) {
        model.addAttribute("posts", postRepository.findAll());
        return "admin/post-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        PostForm form = new PostForm();
        form.setStatus(PostStatus.PUBLISHED);
        model.addAttribute("postForm", form);
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/post-form";
    }

    @PostMapping("/save")
    public String savePost(@ModelAttribute("postForm") PostForm form, RedirectAttributes redirectAttributes) {
        Post post;
        if (form.getId() != null) {
            post = postRepository.findById(form.getId()).orElse(new Post());
        } else {
            post = new Post();
        }

        post.setTitle(form.getTitle());
        // Lưu chuỗi HTML toàn bộ từ CKEditor 5 (bao gồm thẻ định dạng <b>, <i>, <img>...)
        post.setContent(form.getContent() != null ? form.getContent() : "");
        post.setExcerpt(form.getExcerpt());
        post.setFeaturedImage(form.getFeaturedImage());
        post.setStatus(form.getStatus() != null ? form.getStatus() : PostStatus.PUBLISHED);

        // Tạo slug tự động nếu chưa nhập
        String slug = form.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = SlugUtil.toSlug(form.getTitle());
        }
        if (slug.isEmpty()) {
            slug = "post-" + System.currentTimeMillis();
        }
        post.setSlug(slug);

        // Gán chuyên mục nếu có
        if (form.getCategoryId() != null) {
            Category category = categoryRepository.findById(form.getCategoryId()).orElse(null);
            post.setCategory(category);
        } else {
            post.setCategory(null);
        }

        postRepository.save(post);
        redirectAttributes.addFlashAttribute("successMessage", "Lưu bài viết thành công!");
        return "redirect:/admin/posts";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Post> optionalPost = postRepository.findById(id);
        if (optionalPost.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết!");
            return "redirect:/admin/posts";
        }
        Post post = optionalPost.get();
        PostForm form = new PostForm();
        form.setId(post.getId());
        form.setTitle(post.getTitle());
        form.setSlug(post.getSlug());
        form.setExcerpt(post.getExcerpt());
        form.setContent(post.getContent()); // Nạp lại mã HTML vào CKEditor 5
        form.setFeaturedImage(post.getFeaturedImage());
        form.setStatus(post.getStatus());
        if (post.getCategory() != null) {
            form.setCategoryId(post.getCategory().getId());
        }

        model.addAttribute("postForm", form);
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/post-form";
    }

    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (postRepository.existsById(id)) {
            postRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bài viết!");
        }
        return "redirect:/admin/posts";
    }

    @GetMapping("/view/{id}")
    public String viewPost(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Post> optionalPost = postRepository.findById(id);
        if (optionalPost.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết!");
            return "redirect:/admin/posts";
        }
        model.addAttribute("post", optionalPost.get());
        return "admin/post-view";
    }

    @PostMapping("/api/upload-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("upload") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        if (file.isEmpty()) {
            response.put("error", Map.of("message", "File tải lên không hợp lệ."));
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID() + extension;
            Path targetPath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Phản hồi JSON theo chuẩn SimpleUploadAdapter của CKEditor 5: { "url": "..." }
            response.put("url", "/uploads/" + fileName);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("error", Map.of("message", "Lỗi tải ảnh: " + e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Import trực tiếp tệp XML có sẵn trong dự án: vigereduvn.WordPress.2026-10-14.xml
     */
    @PostMapping("/import-default-xml")
    public String importDefaultXml(RedirectAttributes redirectAttributes) {
        try {
            File xmlFile = new File("vigereduvn.WordPress.2026-10-14.xml");
            if (!xmlFile.exists()) {
                xmlFile = new File("spn-viger-app/vigereduvn.WordPress.2026-10-14.xml");
            }
            if (!xmlFile.exists()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy file vigereduvn.WordPress.2026-10-14.xml");
                return "redirect:/admin/posts";
            }

            WordPressXmlImportService.ImportSummary summary = importService.importFromXmlFile(xmlFile);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Import thành công! Đã nạp %d bài viết và %d chuyên mục mới từ tệp XML.",
                            summary.getPostsImported(), summary.getCategoriesImported()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi import XML: " + e.getMessage());
        }
        return "redirect:/admin/posts";
    }

    /**
     * Upload và Import file XML bất kỳ từ máy tính
     */
    @PostMapping("/import-xml-file")
    public String importUploadedXml(@RequestParam("xmlFile") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file XML để tải lên!");
            return "redirect:/admin/posts";
        }
        try {
            WordPressXmlImportService.ImportSummary summary = importService.importFromInputStream(file.getInputStream());
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Import thành công! Đã nạp %d bài viết và %d chuyên mục mới từ %s.",
                            summary.getPostsImported(), summary.getCategoriesImported(), file.getOriginalFilename()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi import XML: " + e.getMessage());
        }
        return "redirect:/admin/posts";
    }
}
