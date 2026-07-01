package com.treehole.pojo;

import lombok.Data;

@Data
public class AiAuditResult {
    /** 0=接口失败转人工/待审核，1=通过，2=驳回 */
    private Integer status;
    private String reason;
    private String risk;

    public static AiAuditResult approved(String reason, String risk) {
        AiAuditResult result = new AiAuditResult();
        result.setStatus(1);
        result.setReason(reason == null || reason.trim().isEmpty() ? "AI审核通过" : reason.trim());
        result.setRisk(risk == null || risk.trim().isEmpty() ? "normal" : risk.trim());
        return result;
    }

    public static AiAuditResult rejected(String reason, String risk) {
        AiAuditResult result = new AiAuditResult();
        result.setStatus(2);
        result.setReason(reason == null || reason.trim().isEmpty() ? "AI判定内容不适合展示" : reason.trim());
        result.setRisk(risk == null || risk.trim().isEmpty() ? "risk" : risk.trim());
        return result;
    }

    public static AiAuditResult pending(String reason) {
        AiAuditResult result = new AiAuditResult();
        result.setStatus(0);
        result.setReason(reason == null || reason.trim().isEmpty() ? "AI审核失败，转人工复核" : reason.trim());
        result.setRisk("pending");
        return result;
    }
}
