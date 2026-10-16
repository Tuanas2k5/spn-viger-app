package com.spn.viger_app.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
        
        if (category != null) {
            posts = postRepository.findByCategory(category);
        }

        // 3. Truyền dữ liệu xuống file HTML (overview.html)
        model.addAttribute("posts", posts);
        
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1); 

        return "education/overview";
    }
}
