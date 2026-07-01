package com.treehole.pojo;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Post {
    private String id;
    private String title;
    private String body;
    private String category;
    private String mood;
    private String alias;
    private String authorUsername;
    private List<String> tags;
    private List<Map<String, String>> media;
    private Integer likes;
    private List<String> likedBy;
    private Integer hugs;
    private List<String> huggedBy;
    private Integer reports;
    private List<String> reportedBy;
    private Date createdAt;

    /** 0=待审核，1=已通过，2=已驳回 */
    private Integer auditStatus;
    private String auditReason;
    private String auditedBy;
    private Date auditedAt;
    private Integer isDeleted;
    private Integer isPinned;

    private List<Reply> replies;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<Map<String, String>> getMedia() {
        return media;
    }

    public void setMedia(List<Map<String, String>> media) {
        this.media = media;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public List<String> getLikedBy() {
        return likedBy;
    }

    public void setLikedBy(List<String> likedBy) {
        this.likedBy = likedBy;
    }

    public Integer getHugs() {
        return hugs;
    }

    public void setHugs(Integer hugs) {
        this.hugs = hugs;
    }

    public List<String> getHuggedBy() {
        return huggedBy;
    }

    public void setHuggedBy(List<String> huggedBy) {
        this.huggedBy = huggedBy;
    }

    public Integer getReports() {
        return reports;
    }

    public void setReports(Integer reports) {
        this.reports = reports;
    }

    public List<String> getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(List<String> reportedBy) {
        this.reportedBy = reportedBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(Integer auditStatus) {
        this.auditStatus = auditStatus;
    }

    public String getAuditReason() {
        return auditReason;
    }

    public void setAuditReason(String auditReason) {
        this.auditReason = auditReason;
    }

    public String getAuditedBy() {
        return auditedBy;
    }

    public void setAuditedBy(String auditedBy) {
        this.auditedBy = auditedBy;
    }

    public Date getAuditedAt() {
        return auditedAt;
    }

    public void setAuditedAt(Date auditedAt) {
        this.auditedAt = auditedAt;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Integer getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Integer isPinned) {
        this.isPinned = isPinned;
    }

    public List<Reply> getReplies() {
        return replies;
    }

    public void setReplies(List<Reply> replies) {
        this.replies = replies;
    }
}
