package com.smartmail.internal.dto;

public record InternalMailActionRequest(
        Long userId,
        Long itemId,
        Long mailItemId,
        String action,
        String type,
        String value,
        String priority,
        Boolean read,
        String folder,
        Long categoryId
) {
    public Long resolvedItemId() {
        return itemId != null ? itemId : mailItemId;
    }

    public String resolvedAction() {
        return action != null ? action : type;
    }
}
