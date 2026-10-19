package com.spn.viger_app.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.spn.viger_app.entity.Category;
import com.spn.viger_app.entity.Post;
import com.spn.viger_app.repository.CategoryRepository;
import com.spn.viger_app.repository.PostRepository;

@Controller
public class BlogController {
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping("/giao-duc-tai-duc/tong-quan")
    public String showOverviewPage(Model model, 
                                   @RequestParam(defaultValue = "1") int page) {
        
        Optional<Category> category = categoryRepository.findBySlug("tong-quan");

        List<Post> posts = new ArrayList<>();
        
        if (category.isPresent()) {
            posts = postRepository.findByCategory(category.get());
        }

        // 3. Truyền dữ liệu xuống file HTML (overview.html)
        model.addAttribute("posts", posts);
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); 

        return "education/overview";
    }

    @GetMapping("/bai-viet/{slug}")
    public String showPostDetail(@PathVariable("slug") String slug, Model model) {
        Optional<Post> postOpt = postRepository.findBySlug(slug);
        if (postOpt.isEmpty()) {
            return "redirect:/blog";
        }

        Post post = postOpt.get();
        model.addAttribute("post", post);

        // Lấy danh sách bài viết liên quan (cùng chuyên mục hoặc bài viết khác)
        List<Post> relatedPosts = new ArrayList<>();
        if (post.getCategory() != null) {
            relatedPosts = postRepository.findByCategory(post.getCategory()).stream()
                    .filter(p -> !p.getId().equals(post.getId()))
                    .limit(5)
                    .toList();
        }
        if (relatedPosts.isEmpty()) {
            relatedPosts = postRepository.findAll().stream()
                    .filter(p -> !p.getId().equals(post.getId()))
                    .limit(5)
                    .toList();
        }
        model.addAttribute("relatedPosts", relatedPosts);

        return "post-detail";
    }
}
