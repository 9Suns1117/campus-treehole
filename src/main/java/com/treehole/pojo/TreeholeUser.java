package com.treehole.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class TreeholeUser {
    private Long userId;
    private String username;
    private String password;
    private String nickname;
    private Integer role; // 1=user, 2=admin
    private String avatarUrl;
    private Integer status; // 1=active, 0=banned
    private Integer muteStatus; // 1=muted, 0=normal
    private Date muteUntil;
    private Date createTime;
    private Integer isDeleted;
}
