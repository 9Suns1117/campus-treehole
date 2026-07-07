package com.treehole.pojo;

import java.util.Date;

public class ListenerProfile {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String reason;
    private String bio;
    private String topics;
    private String availableTime;
    private Integer status;
    private String auditReason;
    private String auditedBy;
    private Date auditedAt;
    private Integer listenCount;
    private Integer thanksCount;
    private Integer warmth;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getTopics() { return topics; }
    public void setTopics(String topics) { this.topics = topics; }
    public String getAvailableTime() { return availableTime; }
    public void setAvailableTime(String availableTime) { this.availableTime = availableTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getAuditReason() { return auditReason; }
    public void setAuditReason(String auditReason) { this.auditReason = auditReason; }
    public String getAuditedBy() { return auditedBy; }
    public void setAuditedBy(String auditedBy) { this.auditedBy = auditedBy; }
    public Date getAuditedAt() { return auditedAt; }
    public void setAuditedAt(Date auditedAt) { this.auditedAt = auditedAt; }
    public Integer getListenCount() { return listenCount; }
    public void setListenCount(Integer listenCount) { this.listenCount = listenCount; }
    public Integer getThanksCount() { return thanksCount; }
    public void setThanksCount(Integer thanksCount) { this.thanksCount = thanksCount; }
    public Integer getWarmth() { return warmth; }
    public void setWarmth(Integer warmth) { this.warmth = warmth; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
