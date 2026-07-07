package com.treehole.pojo;

import java.util.Date;

public class TreeholeUser {
    private Long userId;
    private String username;
    private String password;
    private String nickname;
    private Integer role;
    private String avatarUrl;
    private Integer profilePublic;
    private Integer status;
    private Integer muteStatus;
    private Date muteUntil;
    private Date createTime;
    private Integer isDeleted;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Integer getProfilePublic() { return profilePublic; }
    public void setProfilePublic(Integer profilePublic) { this.profilePublic = profilePublic; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getMuteStatus() { return muteStatus; }
    public void setMuteStatus(Integer muteStatus) { this.muteStatus = muteStatus; }
    public Date getMuteUntil() { return muteUntil; }
    public void setMuteUntil(Date muteUntil) { this.muteUntil = muteUntil; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
