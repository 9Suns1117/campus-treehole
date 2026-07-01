package com.treehole.pojo;

import lombok.Data;
import java.util.Date;

@Data
public class Reply {
    private String id;
    private String postId;
    private String body;
    private Date createdAt;
    private String authorUsername;
    private String alias;

    /** 0=待审核，1=已通过，2=已驳回 */
    private Integer auditStatus;
    private String auditReason;
    private String auditedBy;
    private Date auditedAt;
    private Integer isDeleted;

    /** 管理员审核列表展示用 */
    private String postTitle;
    private String postCategory;
}
