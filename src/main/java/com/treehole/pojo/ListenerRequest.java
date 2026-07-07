package com.treehole.pojo;

import java.util.Date;

public class ListenerRequest {
    private Long id;
    private String listenerUsername;
    private String listenerNickname;
    private String requesterUsername;
    private String requesterNickname;
    private String topic;
    private String message;
    private String responseMode;
    private Integer status;
    private String replyText;
    private Integer thanksSent;
    private Date createdAt;
    private Date acceptedAt;
    private Date repliedAt;
    private Date completedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getListenerUsername() { return listenerUsername; }
    public void setListenerUsername(String listenerUsername) { this.listenerUsername = listenerUsername; }
    public String getListenerNickname() { return listenerNickname; }
    public void setListenerNickname(String listenerNickname) { this.listenerNickname = listenerNickname; }
    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }
    public String getRequesterNickname() { return requesterNickname; }
    public void setRequesterNickname(String requesterNickname) { this.requesterNickname = requesterNickname; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getResponseMode() { return responseMode; }
    public void setResponseMode(String responseMode) { this.responseMode = responseMode; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }
    public Integer getThanksSent() { return thanksSent; }
    public void setThanksSent(Integer thanksSent) { this.thanksSent = thanksSent; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Date acceptedAt) { this.acceptedAt = acceptedAt; }
    public Date getRepliedAt() { return repliedAt; }
    public void setRepliedAt(Date repliedAt) { this.repliedAt = repliedAt; }
    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
}
