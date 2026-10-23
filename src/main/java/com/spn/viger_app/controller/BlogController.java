package com.spn.viger_app.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.data.domain.*;

import com.spn.viger_app.entity.Category;
import com.spn.viger_app.entity.Post;
import com.spn.viger_app.entity.PostStatus;
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
        model.addAttribute("latestPosts", postRepository.findTop5ByOrderByCreatedAtDesc());

        return "education/overview";
    }

    @GetMapping("/giao-duc-tai-duc/he-thong-giao-duc")
    public String educationSystem(Model model, 
                                   @RequestParam(defaultValue = "1") int page) {
        Optional<Category> category = categoryRepository.findBySlug("he-thong-giao-duc");

        List<Post> posts = new ArrayList<>();
        
        if (category.isPresent()) {
            posts = postRepository.findByCategory(category.get());
        }

        model.addAttribute("posts", posts);
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); 
        model.addAttribute("latestPosts", postRepository.findTop5ByOrderByCreatedAtDesc());

        return "education/education-system";
    }

    @GetMapping("/giao-duc-tai-duc/nghien-cuu-va-ung-dung")
    public String researchApplied(Model model, 
                                   @RequestParam(defaultValue = "1") int page) {
        Optional<Category> category = categoryRepository.findBySlug("nghien-cuu-va-ung-dung");

        List<Post> posts = new ArrayList<>();
        
        if (category.isPresent()) {
            posts = postRepository.findByCategory(category.get());
        }

        model.addAttribute("posts", posts);
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); 
        model.addAttribute("latestPosts", postRepository.findTop5ByOrderByCreatedAtDesc());

        return "education/research-applied";
    }

    @GetMapping("/giao-duc-tai-duc/chi-phi-va-co-hoi")
    public String costsOpportunities(Model model, 
                                   @RequestParam(defaultValue = "1") int page) {
        Optional<Category> category = categoryRepository.findBySlug("chi-phi-va-co-hoi");

        List<Post> posts = new ArrayList<>();
        
        if (category.isPresent()) {
            posts = postRepository.findByCategory(category.get());
        }

        model.addAttribute("posts", posts);
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); 
        model.addAttribute("latestPosts", postRepository.findTop5ByOrderByCreatedAtDesc());

        return "education/costs-opportunities";
    }

    @GetMapping("/giao-duc-tai-duc/co-hoi-nghe-nghiep")
    public String careerOpportunities(Model model, 
                                   @RequestParam(defaultValue = "1") int page) {
        Optional<Category> category = categoryRepository.findBySlug("co-hoi-nghe-nghiep");

        List<Post> posts = new ArrayList<>();
        
        if (category.isPresent()) {
            posts = postRepository.findByCategory(category.get());
        }

        model.addAttribute("posts", posts);
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); 
        model.addAttribute("latestPosts", postRepository.findTop5ByOrderByCreatedAtDesc());

        return "education/career-opportunities";
    }

    @GetMapping("/tu-van/du-hoc-dai-hoc")
    public String consultBachelor(Model model, 
                                   @RequestParam(defaultValue = "1") int page) {
        Optional<Category> category = categoryRepository.findBySlug("du-hoc-dai-hoc");

        List<Post> posts = new ArrayList<>();
        
        if (category.isPresent()) {
            posts = postRepository.findByCategory(category.get());
        }

        model.addAttribute("posts", posts);
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); 
        model.addAttribute("latestPosts", postRepository.findTop5ByOrderByCreatedAtDesc());

        return "consult/consult-bachelor";
    }

    @GetMapping("/tu-van/du-hoc-cao-hoc")
    public String consultMaster(Model model, 
                                   @RequestParam(defaultValue = "1") int page) {
        Optional<Category> category = categoryRepository.findBySlug("du-hoc-cao-hoc");

        List<Post> posts = new ArrayList<>();
        
        if (category.isPresent()) {
            posts = postRepository.findByCategory(category.get());
        }

        model.addAttribute("posts", posts);
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); 
        model.addAttribute("latestPosts", postRepository.findTop5ByOrderByCreatedAtDesc());

        return "consult/consult-master";
    }

    @GetMapping("/chuong-trinh-hoc")
    public String programs(Model model, 
                                   @RequestParam(defaultValue = "1") int page) {
        Optional<Category> category = categoryRepository.findBySlug("chuong-trinh-hoc");

        List<Post> posts = new ArrayList<>();
        
        if (category.isPresent()) {
            posts = postRepository.findByCategory(category.get());
        }

        model.addAttribute("posts", posts);
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); 
        model.addAttribute("latestPosts", postRepository.findTop5ByOrderByCreatedAtDesc());

        return "programs";
    }

    @GetMapping("/blog/hieu-ve-duc")
    public String understandGermany(Model model, 
                                   @RequestParam(defaultValue = "1") int page) {
        Optional<Category> category = categoryRepository.findBySlug("hieu-ve-duc");

        List<Post> posts = new ArrayList<>();
        
        if (category.isPresent()) {
            posts = postRepository.findByCategory(category.get());
        }

        model.addAttribute("posts", posts);
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); 
        model.addAttribute("latestPosts", postRepository.findTop5ByOrderByCreatedAtDesc());

        return "blog/understand-germany";
    }

    @GetMapping("/blog/cuoc-song-o-duc")
    public String lifeInGermany(Model model, 
                                   @RequestParam(defaultValue = "1") int page) {
        Optional<Category> category = categoryRepository.findBySlug("cuoc-song-o-duc");

        List<Post> posts = new ArrayList<>();
        
        if (category.isPresent()) {
            posts = postRepository.findByCategory(category.get());
        }

        model.addAttribute("posts", posts);
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); 
        model.addAttribute("latestPosts", postRepository.findTop5ByOrderByCreatedAtDesc());

        return "blog/life-in-germany";
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

    @GetMapping("/blog")
    public String blog(@RequestParam(defaultValue = "1") int page,
                    @RequestParam(required = false) Long category,
                    @RequestParam(required = false) String q,
                    Model model) {

        String keyword = (q == null) ? "" : q.trim();
        boolean filtering = category != null || !keyword.isEmpty();

        // Bài nổi bật: chỉ khi không lọc/tìm kiếm
        Post featured = null;
        if (!filtering) {
            featured = postRepository
                    .findFirstByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED).orElse(null);
        }

        Page<Post> result = postRepository.searchPosts(
                PostStatus.PUBLISHED, category, keyword,
                featured != null ? featured.getId() : null,
                PageRequest.of(Math.max(page, 1) - 1, 9, Sort.by("createdAt").descending()));

        // Chuỗi query giữ lại bộ lọc khi chuyển trang
        String query = UriComponentsBuilder.newInstance()
                .queryParamIfPresent("category", Optional.ofNullable(category))
                .queryParamIfPresent("q", Optional.of(keyword).filter(s -> !s.isEmpty()))
                .build().encode().getQuery();

        model.addAttribute("featured", featured);
        model.addAttribute("posts", result.getContent());
        model.addAttribute("currentPage", Math.max(page, 1));
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("activeCategory", category);
        model.addAttribute("q", keyword);
        model.addAttribute("filterQuery", query == null ? "" : "&" + query);
        return "blog"; 
    }
}
