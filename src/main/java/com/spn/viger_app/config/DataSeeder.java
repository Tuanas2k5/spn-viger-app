package com.spn.viger_app.config;

import com.spn.viger_app.entity.Category;
import com.spn.viger_app.entity.Users;
import com.spn.viger_app.repository.CategoryRepository;
import com.spn.viger_app.repository.UsersRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner initDatabase(CategoryRepository categoryRepo,
                                          UsersRepository usersRepo,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            // ==================== Nạp chuyên mục ====================
            List<Category> initialCategories = Arrays.asList(
                createCategory("Uncategorized", "uncategorized"),
                createCategory("Tổng quan", "tong-quan"),
                createCategory("Hệ thống giáo dục", "he-thong-giao-duc"),
                createCategory("Nghiên cứu và ứng dụng", "nghien-cuu-va-ung-dung"),
                createCategory("Chi phí và cơ hội", "chi-phi-va-co-hoi"),
                createCategory("Cơ hội nghề nghiệp", "co-hoi-nghe-nghiep"),
                createCategory("Du học Đại học", "du-hoc-dai-hoc"),
                createCategory("Du học Cao học", "du-hoc-cao-hoc"),
                createCategory("Chương trình học", "chuong-trinh-hoc"),
                createCategory("Hiểu về Đức", "hieu-ve-duc"),
                createCategory("Cuộc sống ở Đức", "cuoc-song-o-duc")
            );

            for (Category category : initialCategories) {
                if (categoryRepo.findBySlug(category.getSlug()).isEmpty()) {
                    categoryRepo.save(category);
                    System.out.println("Đã thêm chuyên mục mới vào CSDL: " + category.getName());
                }
            }

            // ==================== Nạp tài khoản admin ====================
            if (usersRepo.findByUsername("SPN").isEmpty()) {
                Users admin = Users.builder()
                        .username("SPN")
                        .password(passwordEncoder.encode("S4ophu0ngn4m"))
                        .fullName("Quản trị SPN")
                        .role("ADMIN")
                        .active(true)
                        .build();

                usersRepo.save(admin);
                System.out.println("Đã tạo tài khoản admin mặc định (username: SPN)");
            }
        };
    }

    // Hàm phụ để tạo nhanh đối tượng Category
    private Category createCategory(String name, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        return category;
    }
}