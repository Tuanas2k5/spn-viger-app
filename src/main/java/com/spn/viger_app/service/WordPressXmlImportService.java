package com.spn.viger_app.service;

import com.spn.viger_app.dto.xml.CategoryXml;
import com.spn.viger_app.dto.xml.ItemXml;
import com.spn.viger_app.dto.xml.RssXml;
import com.spn.viger_app.entity.Category;
import com.spn.viger_app.entity.Post;
import com.spn.viger_app.entity.PostStatus;
import com.spn.viger_app.repository.CategoryRepository;
import com.spn.viger_app.repository.PostRepository;
import com.spn.viger_app.util.SlugUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


@Service
public class WordPressXmlImportService {

    private static final Logger log = LoggerFactory.getLogger(WordPressXmlImportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern WP_BLOCK_COMMENT_PATTERN = Pattern.compile("<!--\\s*/?wp:[\\s\\S]*?-->");

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;

    private final String UPLOAD_DIR = "src/main/resources/static/img/uploads";
    private final String URL_PREFIX = "/img/uploads/";

    public WordPressXmlImportService(PostRepository postRepository, CategoryRepository categoryRepository) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Import dữ liệu từ file XML trên đường dẫn hệ thống
     */
    @Transactional
    public ImportSummary importFromXmlFile(File xmlFile) throws Exception {
        log.info("Bắt đầu đọc file XML từ: {}", xmlFile.getAbsolutePath());
        JAXBContext context = JAXBContext.newInstance(RssXml.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        RssXml rss = (RssXml) unmarshaller.unmarshal(xmlFile);
        return processRss(rss);
    }

    /**
     * Import dữ liệu từ InputStream (hỗ trợ upload file từ Admin Web)
     */
    @Transactional
    public ImportSummary importFromInputStream(InputStream inputStream) throws Exception {
        JAXBContext context = JAXBContext.newInstance(RssXml.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        RssXml rss = (RssXml) unmarshaller.unmarshal(inputStream);
        return processRss(rss);
    }

    /**
     * Xử lý bóc tách dữ liệu từ đối tượng JAXB và lưu vào MySQL
     */
    private ImportSummary processRss(RssXml rss) {
        if (rss == null || rss.getChannel() == null || rss.getChannel().getItems() == null) {
            return new ImportSummary(0, 0, 0, 0, Collections.emptyList());
        }

        List<ItemXml> items = rss.getChannel().getItems();
        log.info("Tổng số item đọc được từ XML: {}", items.size());

        // 1. Quét trước các file đính kèm (attachment) để lấy map: Post ID -> URL ảnh đại diện
        Map<Long, String> attachmentMap = new HashMap<>();
        for (ItemXml item : items) {
            if ("attachment".equalsIgnoreCase(item.getPostType()) && item.getPostId() != null) {
                if (item.getAttachmentUrl() != null && !item.getAttachmentUrl().trim().isEmpty()) {
                    attachmentMap.put(item.getPostId(), item.getAttachmentUrl().trim());
                }
            }
        }
        log.info("Đã tìm thấy {} tệp ảnh đính kèm (attachments)", attachmentMap.size());

        // Cache Category theo slug để tối ưu lượt query DB
        Map<String, Category> categoryCache = new HashMap<>();
        List<String> importedPostTitles = new ArrayList<>();
        int postCount = 0;
        int categoryCount = 0;

        // 2. Quét qua các bài viết (post)
        for (ItemXml item : items) {
            if (!"post".equalsIgnoreCase(item.getPostType())) {
                continue; // Bỏ qua trang (page), tệp đính kèm (attachment)...
            }

            // --- Bóc tách Category ---
            Category postCategory = null;
            CategoryXml catXml = item.getPrimaryCategory();
            if (catXml != null && catXml.getName() != null && !catXml.getName().trim().isEmpty()) {
                String catSlug = catXml.getNicename();
                if (catSlug == null || catSlug.trim().isEmpty()) {
                    catSlug = SlugUtil.toSlug(catXml.getName());
                }

                if (categoryCache.containsKey(catSlug)) {
                    postCategory = categoryCache.get(catSlug);
                } else {
                    final String finalSlug = catSlug;
                    final String finalName = catXml.getName().trim();
                    Optional<Category> existingCat = categoryRepository.findBySlug(finalSlug);
                    if (existingCat.isPresent()) {
                        postCategory = existingCat.get();
                    } else {
                        Category newCat = Category.builder()
                                .name(finalName)
                                .slug(finalSlug)
                                .build();
                        postCategory = categoryRepository.save(newCat);
                        categoryCount++;
                        log.info("Đã tạo mới Category: {} ({})", finalName, finalSlug);
                    }
                    categoryCache.put(catSlug, postCategory);
                }
            }

            // --- Bóc tách Post ---
            String title = item.getTitle() != null ? item.getTitle().trim() : "Không có tiêu đề";
            String slug = item.getPostName();
            if (slug == null || slug.trim().isEmpty()) {
                slug = SlugUtil.toSlug(title);
            }
            if (slug.isEmpty()) {
                slug = "post-" + (item.getPostId() != null ? item.getPostId() : System.currentTimeMillis());
            }

            // Làm sạch nội dung HTML: loại bỏ các thẻ comment Gutenberg block <!-- wp:... -->
            String rawContent = item.getContent() != null ? item.getContent() : "";
            String cleanHtmlContent = cleanWordPressHtml(rawContent);

            // Lấy tóm tắt
            String excerpt = item.getExcerpt() != null ? item.getExcerpt().trim() : "";
            if (excerpt.length() > 1000) {
                excerpt = excerpt.substring(0, 997) + "...";
            }

            // Ngày tạo
            LocalDateTime createdAt = parsePostDate(item.getPostDate());

            // Trạng thái
            PostStatus status = "publish".equalsIgnoreCase(item.getStatus()) ? PostStatus.PUBLISHED : PostStatus.DRAFT;

            // Ảnh đại diện từ _thumbnail_id
            String featuredImage = null;
            Long thumbnailId = item.getThumbnailId();
            if (thumbnailId != null && attachmentMap.containsKey(thumbnailId)) {
                featuredImage = attachmentMap.get(thumbnailId);
            }

            // Kiểm tra xem bài viết đã tồn tại theo slug chưa để tránh trùng lặp
            Optional<Post> existingPostOpt = postRepository.findBySlug(slug);
            Post post = existingPostOpt.orElse(new Post());

            post.setTitle(title);
            post.setSlug(slug);
            String finalContent = processAndDownloadImages(cleanHtmlContent);
            post.setContent(finalContent);
            post.setExcerpt(excerpt);
            post.setCreatedAt(createdAt);
            post.setStatus(status);
            post.setFeaturedImage(featuredImage);
            post.setCategory(postCategory);

            postRepository.save(post);
            postCount++;
            importedPostTitles.add(title);
            log.info("Đã lưu bài viết: {} (slug: {})", title, slug);
        }

        log.info("Import hoàn tất: {} bài viết, {} chuyên mục mới", postCount, categoryCount);
        return new ImportSummary(items.size(), postCount, categoryCount, attachmentMap.size(), importedPostTitles);
    }

    /**
     * Làm sạch mã HTML Gutenberg của WordPress (giữ lại các thẻ <b>, <i>, <img>, <p>, <ul>...)
     */
    private String cleanWordPressHtml(String rawHtml) {
        if (rawHtml == null) {
            return "";
        }
        // Xóa các chú thích block comment dạng <!-- wp:paragraph --> và <!-- /wp:paragraph -->
        String cleaned = WP_BLOCK_COMMENT_PATTERN.matcher(rawHtml).replaceAll("");
        return cleaned.trim();
    }

    /**
     * Parse chuỗi ngày tháng từ WordPress
     */
    private LocalDateTime parsePostDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Không thể parse ngày '{}', dùng thời gian hiện tại", dateStr);
            return LocalDateTime.now();
        }
    }

    /**
     * DTO chứa thông tin kết quả Import
     */
    public static class ImportSummary {
        private final int totalItems;
        private final int postsImported;
        private final int categoriesImported;
        private final int attachmentsFound;
        private final List<String> postTitles;

        public ImportSummary(int totalItems, int postsImported, int categoriesImported, int attachmentsFound, List<String> postTitles) {
            this.totalItems = totalItems;
            this.postsImported = postsImported;
            this.categoriesImported = categoriesImported;
            this.attachmentsFound = attachmentsFound;
            this.postTitles = postTitles;
        }

        public int getTotalItems() {
            return totalItems;
        }

        public int getPostsImported() {
            return postsImported;
        }

        public int getCategoriesImported() {
            return categoriesImported;
        }

        public int getAttachmentsFound() {
            return attachmentsFound;
        }

        public List<String> getPostTitles() {
            return postTitles;
        }
    }

    /**
     * Hàm xử lý chuỗi HTML nội dung bài viết (Content)
     * Tìm link ảnh WordPress cũ, tải về và thay bằng link Local mới.
     */
    public String processAndDownloadImages(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }

        // Biểu thức Regex tìm tất cả các thẻ <img src="...">
        String regex = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        StringBuffer updatedContent = new StringBuffer();

        // Tạo thư mục nếu chưa tồn tại
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (Exception e) {
            System.err.println("Lỗi tạo thư mục lưu ảnh: " + e.getMessage());
            return content;
        }

        // Quét từng link ảnh trong nội dung bài
        while (matcher.find()) {
            // Lấy đường dẫn URL gốc (Group 1 của Regex)
            String originalImageUrl = matcher.group(1);
            
            try {
                // Chỉ xử lý các link từ web ngoài (http/https)
                if (originalImageUrl.startsWith("http")) {
                    
                    // 1. Tải ảnh về ổ cứng và nhận lại tên file mới (đã đổi tên chống trùng lặp)
                    String newFileName = downloadImageLocally(originalImageUrl);
                    
                    // 2. Tạo đường dẫn src cục bộ mới
                    String newLocalUrl = URL_PREFIX + newFileName;
                    
                    // 3. Thay thế URL cũ bằng URL mới ngay bên trong thẻ <img> hiện tại
                    String replacedImgTag = matcher.group(0).replace(originalImageUrl, newLocalUrl);
                    
                    matcher.appendReplacement(updatedContent, replacedImgTag);
                    System.out.println("✅ Tải thành công: " + originalImageUrl + " -> " + newLocalUrl);
                } else {
                    matcher.appendReplacement(updatedContent, matcher.group(0)); // Bỏ qua nếu không phải link http
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi tải ảnh [" + originalImageUrl + "]: " + e.getMessage());
                matcher.appendReplacement(updatedContent, matcher.group(0)); // Lỗi thì giữ nguyên link cũ
            }
        }
        
        // Gắn phần văn bản còn lại sau khi đã thay thế hết ảnh
        matcher.appendTail(updatedContent);

        return updatedContent.toString();
    }

    /**
     * Hàm phụ: Tải một file từ URL về thư mục UPLOAD_DIR
     */
    private String downloadImageLocally(String imageUrl) throws Exception {
        URL url = new URL(imageUrl);
        
        // Bóc tách lấy đuôi file gốc (VD: .jpg, .png)
        String originalFileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        if(originalFileName.contains("?")) { // Xóa bỏ query parameter nếu có (?v=123)
            originalFileName = originalFileName.substring(0, originalFileName.indexOf("?"));
        }

        String extension = "";
        int dotIndex = originalFileName.lastIndexOf(".");
        if(dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
        }
        
        // Dùng UUID để tạo chuỗi ngẫu nhiên làm tên file, tránh bị trùng lặp tên làm ghi đè ảnh cũ
        String newFileName = UUID.randomUUID().toString() + extension;
        
        Path destinationPath = Paths.get(UPLOAD_DIR).resolve(newFileName);

        // Mở luồng tải file và lưu đè nếu đã tồn tại
        try (InputStream in = url.openStream()) {
            Files.copy(in, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return newFileName;
    }

    // Hàm phụ để kiểm tra và gán chuyên mục mới dựa trên tên chuyên mục cũ
    private Optional<Category> mapToNewCategory(String oldCategoryName, CategoryRepository categoryRepo) {
        String newSlug = "uncategorized"; // Mặc định nếu không map được

        // Chuyển tên cũ sang chữ thường để dễ so sánh (bỏ qua viết hoa/thường)
        String lowerOldName = oldCategoryName.toLowerCase();

        // 1. Tổng quan
        if (lowerOldName.contains("overview") || lowerOldName.contains("tổng quan")) {
            newSlug = "tong-quan";
        } 
        // 2. Hệ thống giáo dục
        else if (lowerOldName.contains("education system") || lowerOldName.contains("hệ thống") || lowerOldName.contains("he thong")) {
            newSlug = "he-thong-giao-duc";
        } 
        // 3. Nghiên cứu và ứng dụng
        else if (lowerOldName.contains("research") || lowerOldName.contains("applied") || lowerOldName.contains("nghiên cứu") || lowerOldName.contains("ứng dụng")) {
            newSlug = "nghien-cuu-va-ung-dung";
        } 
        // 4. Chi phí và cơ hội (Học bổng, chứng minh tài chính)
        else if (lowerOldName.contains("cost") || lowerOldName.contains("chi phí") || lowerOldName.contains("học bổng") || lowerOldName.contains("scholarship")) {
            newSlug = "chi-phi-va-co-hoi";
        } 
        // 5. Cơ hội nghề nghiệp (Việc làm, thẻ xanh)
        else if (lowerOldName.contains("career") || lowerOldName.contains("job") || lowerOldName.contains("cơ hội nghề nghiệp") || lowerOldName.contains("việc làm")) {
            newSlug = "co-hoi-nghe-nghiep";
        } 
        // 6. Du học Đại học (Bachelor)
        else if (lowerOldName.contains("bachelor") || lowerOldName.contains("đại học") || lowerOldName.contains("dai hoc")) {
            newSlug = "du-hoc-dai-hoc";
        } 
        // 7. Du học Cao học (Master, PhD)
        else if (lowerOldName.contains("master") || lowerOldName.contains("phd") || lowerOldName.contains("cao học") || lowerOldName.contains("thạc sĩ")) {
            newSlug = "du-hoc-cao-hoc";
        } 
        // 8. Chương trình học (Ngành nghề)
        else if (lowerOldName.contains("program") || lowerOldName.contains("chương trình") || lowerOldName.contains("ngành học")) {
            newSlug = "chuong-trinh-hoc";
        } 
        // 9. Hiểu về Đức (Văn hóa, lịch sử)
        else if (lowerOldName.contains("understand") || lowerOldName.contains("culture") || lowerOldName.contains("hiểu về") || lowerOldName.contains("văn hóa")) {
            newSlug = "hieu-ve-duc";
        } 
        // 10. Cuộc sống ở Đức (Life in Germany)
        else if (lowerOldName.contains("life in germany") || lowerOldName.contains("cuộc sống") || lowerOldName.contains("sinh hoạt")) {
            newSlug = "cuoc-song-o-duc";
        }

        // Tìm Category trong Database dựa theo Slug mới
        Optional<Category> category = categoryRepo.findBySlug(newSlug);
        
        // Nếu trong Database mới chưa tạo Category này (hoặc bị sai slug), gán tạm vào Uncategorized
        if (category.isEmpty()) {
            category = categoryRepo.findBySlug("uncategorized");
        }
        
        return category;
    }
}
