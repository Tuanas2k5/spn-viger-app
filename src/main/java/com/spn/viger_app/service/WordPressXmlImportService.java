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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class WordPressXmlImportService {

    private static final Logger log = LoggerFactory.getLogger(WordPressXmlImportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern WP_BLOCK_COMMENT_PATTERN = Pattern.compile("<!--\\s*/?wp:[\\s\\S]*?-->");

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;

    private final String UPLOAD_DIR = "src/main/resources/static/img/uploads";
    private final String URL_PREFIX = "/img/uploads/";

    // Cache tránh tải lại cùng một URL nhiều lần
    private final Map<String, String> downloadedUrlCache = new ConcurrentHashMap<>();

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

            // Ảnh đại diện từ _thumbnail_id hoặc lấy từ ảnh đầu tiên của bài viết
            String featuredImage = null;
            Long thumbnailId = item.getThumbnailId();
            if (thumbnailId != null && attachmentMap.containsKey(thumbnailId)) {
                featuredImage = attachmentMap.get(thumbnailId);
            }
            if (!StringUtils.hasText(featuredImage)) {
                featuredImage = extractFirstImageUrl(cleanHtmlContent);
            }
            if (StringUtils.hasText(featuredImage) && featuredImage.trim().startsWith("http")) {
                try {
                    String localFileName = downloadImageLocally(featuredImage.trim());
                    featuredImage = URL_PREFIX + localFileName;
                    log.info("Đã tải ảnh đại diện bài viết [{}]: {}", title, featuredImage);
                } catch (Exception e) {
                    log.warn("Không thể tải ảnh đại diện [{}]: {}", featuredImage, e.getMessage());
                }
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
    /**
     * Trích xuất URL ảnh đầu tiên xuất hiện trong nội dung HTML
     */
    public String extractFirstImageUrl(String htmlContent) {
        if (!StringUtils.hasText(htmlContent)) {
            return null;
        }
        Pattern pattern = Pattern.compile("<img\\b[^>]*?\\bsrc\\s*=\\s*['\"]([^'\"]+?)['\"][^>]*?>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlContent);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Hàm xử lý chuỗi HTML nội dung bài viết (Content)
     * Tìm link ảnh WordPress cũ, tải về và thay bằng link Local mới.
     * Đồng thời xóa bỏ srcset và sizes cũ để ép trình duyệt load ảnh local.
     */
    public String processAndDownloadImages(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }

        try {
            ensureUploadDirectories();
        } catch (Exception e) {
            log.error("Lỗi tạo thư mục lưu ảnh: {}", e.getMessage());
            return content;
        }

        // Regex tìm toàn bộ thẻ <img ...>
        Pattern imgPattern = Pattern.compile("<img\\b([^>]*?)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = imgPattern.matcher(content);

        StringBuffer updatedContent = new StringBuffer();

        while (matcher.find()) {
            String fullImgTag = matcher.group(0);
            String attributes = matcher.group(1);

            Pattern srcPattern = Pattern.compile("\\bsrc\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
            Matcher srcMatcher = srcPattern.matcher(attributes);

            if (srcMatcher.find()) {
                String originalImageUrl = srcMatcher.group(1).trim();

                if (originalImageUrl.startsWith("http://") || originalImageUrl.startsWith("https://")) {
                    try {
                        String newFileName = downloadImageLocally(originalImageUrl);
                        String newLocalUrl = URL_PREFIX + newFileName;

                        // Thay src cũ bằng src local mới
                        String modifiedTag = srcMatcher.replaceFirst("src=\"" + Matcher.quoteReplacement(newLocalUrl) + "\"");

                        // Xóa thuộc tính srcset và sizes cũ của WordPress để ép browser load src local
                        modifiedTag = modifiedTag.replaceAll("(?i)\\s*srcset\\s*=\\s*['\"][^'\"]*['\"]", "");
                        modifiedTag = modifiedTag.replaceAll("(?i)\\s*sizes\\s*=\\s*['\"][^'\"]*['\"]", "");

                        String finalImgTag = "<img" + modifiedTag + ">";
                        matcher.appendReplacement(updatedContent, Matcher.quoteReplacement(finalImgTag));
                        log.info("✅ Tải thành công ảnh nội dung: {} -> {}", originalImageUrl, newLocalUrl);
                        continue;
                    } catch (Exception e) {
                        log.error("❌ Lỗi tải ảnh [{}]: {}", originalImageUrl, e.getMessage());
                    }
                }
            }

            // Nếu không phải link http hoặc tải lỗi, giữ nguyên thẻ cũ
            matcher.appendReplacement(updatedContent, Matcher.quoteReplacement(fullImgTag));
        }

        matcher.appendTail(updatedContent);
        return updatedContent.toString();
    }

    /**
     * Tải một file từ URL về thư mục UPLOAD_DIR với đầy đủ Header, Timeout và hỗ trợ Redirects
     */
    private String downloadImageLocally(String imageUrl) throws Exception {
        if (!StringUtils.hasText(imageUrl)) {
            throw new IllegalArgumentException("Image URL không được rỗng");
        }

        imageUrl = imageUrl.trim();

        // 1. Kiểm tra cache nếu URL này đã tải trước đó
        if (downloadedUrlCache.containsKey(imageUrl)) {
            return downloadedUrlCache.get(imageUrl);
        }

        ensureUploadDirectories();

        // Chuẩn hóa URL (encode khoảng trắng nếu có)
        String encodedUrl = imageUrl.replace(" ", "%20");

        HttpURLConnection connection = null;
        int redirects = 0;
        int maxRedirects = 5;

        while (redirects < maxRedirects) {
            URL url = URI.create(encodedUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10s
            connection.setReadTimeout(20000);    // 20s
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER || status == 307 || status == 308) {
                String location = connection.getHeaderField("Location");
                if (location != null && !location.isEmpty()) {
                    if (location.startsWith("/")) {
                        URL base = new URL(encodedUrl);
                        encodedUrl = base.getProtocol() + "://" + base.getHost() + location;
                    } else {
                        encodedUrl = location;
                    }
                    redirects++;
                    connection.disconnect();
                    continue;
                }
            }

            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + status + " khi tải " + imageUrl);
            }
            break;
        }

        // Bóc tách lấy extension
        String extension = "";
        String cleanUrl = imageUrl.contains("?") ? imageUrl.substring(0, imageUrl.indexOf("?")) : imageUrl;
        int lastSlash = cleanUrl.lastIndexOf("/");
        String fileNameFromUrl = lastSlash >= 0 ? cleanUrl.substring(lastSlash + 1) : cleanUrl;
        int dotIndex = fileNameFromUrl.lastIndexOf(".");
        if (dotIndex > 0) {
            extension = fileNameFromUrl.substring(dotIndex);
        }

        // Nếu URL không có extension thì suy luận từ Content-Type
        if (!StringUtils.hasText(extension) && connection != null) {
            String contentType = connection.getContentType();
            if (contentType != null) {
                contentType = contentType.toLowerCase();
                if (contentType.contains("jpeg") || contentType.contains("jpg")) extension = ".jpg";
                else if (contentType.contains("png")) extension = ".png";
                else if (contentType.contains("webp")) extension = ".webp";
                else if (contentType.contains("gif")) extension = ".gif";
                else if (contentType.contains("svg")) extension = ".svg";
            }
        }
        if (!StringUtils.hasText(extension)) {
            extension = ".jpg";
        }

        String newFileName = UUID.randomUUID().toString() + extension;
        Path destinationPath = Paths.get(UPLOAD_DIR).resolve(newFileName);

        // Mở luồng tải file và ghi vào thư mục upload
        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Đồng thời copy vào target/classes/static/img/uploads (nếu có) để Spring Boot phục vụ ngay
        try {
            Path targetUploadDir = Paths.get("target/classes/static/img/uploads");
            if (Files.exists(targetUploadDir)) {
                Files.copy(destinationPath, targetUploadDir.resolve(newFileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {
        }

        downloadedUrlCache.put(imageUrl, newFileName);
        return newFileName;
    }

    private void ensureUploadDirectories() throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Path targetUploadDir = Paths.get("target/classes/static/img/uploads");
        if (Files.exists(Paths.get("target/classes/static")) && !Files.exists(targetUploadDir)) {
            Files.createDirectories(targetUploadDir);
        }
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
