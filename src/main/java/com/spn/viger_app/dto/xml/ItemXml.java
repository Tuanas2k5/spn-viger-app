package com.spn.viger_app.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ItemXml {

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "encoded", namespace = "http://purl.org/rss/1.0/modules/content/")
    private String content;

    @XmlElement(name = "encoded", namespace = "http://wordpress.org/export/1.2/excerpt/")
    private String excerpt;

    @XmlElement(name = "post_id", namespace = "http://wordpress.org/export/1.2/")
    private Long postId;

    @XmlElement(name = "post_date", namespace = "http://wordpress.org/export/1.2/")
    private String postDate;

    @XmlElement(name = "post_name", namespace = "http://wordpress.org/export/1.2/")
    private String postName;

    @XmlElement(name = "status", namespace = "http://wordpress.org/export/1.2/")
    private String status;

    @XmlElement(name = "post_type", namespace = "http://wordpress.org/export/1.2/")
    private String postType;

    @XmlElement(name = "attachment_url", namespace = "http://wordpress.org/export/1.2/")
    private String attachmentUrl;

    @XmlElement(name = "category")
    private List<CategoryXml> categories = new ArrayList<>();

    @XmlElement(name = "postmeta", namespace = "http://wordpress.org/export/1.2/")
    private List<PostMetaXml> postMetaList = new ArrayList<>();

    public ItemXml() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getPostDate() {
        return postDate;
    }

    public void setPostDate(String postDate) {
        this.postDate = postDate;
    }

    public String getPostName() {
        return postName;
    }

    public void setPostName(String postName) {
        this.postName = postName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public List<CategoryXml> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryXml> categories) {
        this.categories = categories;
    }

    public List<PostMetaXml> getPostMetaList() {
        return postMetaList;
    }

    public void setPostMetaList(List<PostMetaXml> postMetaList) {
        this.postMetaList = postMetaList;
    }

    /**
     * Lấy ID ảnh đại diện từ meta _thumbnail_id
     */
    public Long getThumbnailId() {
        if (postMetaList == null) {
            return null;
        }
        for (PostMetaXml meta : postMetaList) {
            if ("_thumbnail_id".equals(meta.getKey()) && meta.getValue() != null && !meta.getValue().trim().isEmpty()) {
                try {
                    return Long.parseLong(meta.getValue().trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Lấy Category chính (domain="category")
     */
    public CategoryXml getPrimaryCategory() {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        for (CategoryXml cat : categories) {
            if ("category".equalsIgnoreCase(cat.getDomain())) {
                return cat;
            }
        }
        return categories.get(0);
    }
}
