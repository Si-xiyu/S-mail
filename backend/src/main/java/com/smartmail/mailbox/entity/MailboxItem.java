package com.smartmail.mailbox.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("mailbox_item")
public class MailboxItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long mailId;
    private String folder;
    private Boolean readFlag;
    private Boolean starFlag;
    private Boolean deletedFlag;
    private String priority;
    private LocalDateTime receivedAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getMailId() { return mailId; }
    public void setMailId(Long mailId) { this.mailId = mailId; }
    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }
    public Boolean getReadFlag() { return readFlag; }
    public void setReadFlag(Boolean readFlag) { this.readFlag = readFlag; }
    public Boolean getStarFlag() { return starFlag; }
    public void setStarFlag(Boolean starFlag) { this.starFlag = starFlag; }
    public Boolean getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Boolean deletedFlag) { this.deletedFlag = deletedFlag; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
