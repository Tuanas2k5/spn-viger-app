package com.spn.viger_app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Path uploadDir = Paths.get("uploads");
            File dir = uploadDir.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String uploadPath = dir.getAbsolutePath().replace("\\", "/");
            if (!uploadPath.endsWith("/")) {
                uploadPath += "/";
            }

            Path imgUploadDir = Paths.get("src/main/resources/static/img/uploads");
            if (!Files.exists(imgUploadDir)) {
                Files.createDirectories(imgUploadDir);
            }
            String imgPath = imgUploadDir.toFile().getAbsolutePath().replace("\\", "/");
            if (!imgPath.endsWith("/")) imgPath += "/";

            // registry.addResourceHandler("/uploads/**")
            //         .addResourceLocations("file:" + uploadPath);
            registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + Paths.get("uploads").toAbsolutePath() + "/");

            registry.addResourceHandler("/img/uploads/**")
                    .addResourceLocations("file:" + imgPath); 
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục uploads", e);
        }        

    }
}
