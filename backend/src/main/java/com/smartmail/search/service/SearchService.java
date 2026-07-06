package com.smartmail.search.service;

import com.smartmail.common.response.PageResponse;
import com.smartmail.common.security.UserContext;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import com.smartmail.search.dto.SearchResultResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SearchService {

    private final MailboxItemMapper mailboxItemMapper;

    public SearchService(MailboxItemMapper mailboxItemMapper) {
        this.mailboxItemMapper = mailboxItemMapper;
    }

    public PageResponse<SearchResultResponse> search(
            String keyword, String folder, Long categoryId, Boolean starred, long page, long pageSize) {
        Long userId = UserContext.requireUserId();
        String likeKeyword = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String normalizedFolder = normalizeFolder(folder);
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(pageSize, 1), 50);
        long offset = (safePage - 1) * safeSize;

        List<Map<String, Object>> rows = mailboxItemMapper.searchByKeyword(
                userId, likeKeyword, normalizedFolder, categoryId, starred, safeSize, offset);
        long total = mailboxItemMapper.countByKeyword(userId, likeKeyword,
                normalizedFolder, categoryId, starred);

        List<SearchResultResponse> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            records.add(new SearchResultResponse(
                    toLong(column(row, "ID")),
                    toLong(column(row, "MAIL_ID")),
                    (String) column(row, "SENDER_EMAIL"),
                    (String) column(row, "SUBJECT"),
                    snippet((String) column(row, "CONTENT_TEXT"), keyword == null ? "" : keyword),
                    (String) column(row, "FOLDER"),
                    toBoolean(column(row, "READ_FLAG")),
                    toBoolean(column(row, "STAR_FLAG")),
                    (String) column(row, "PRIORITY"),
                    toBoolean(column(row, "HAS_ATTACHMENT")),
                    toLocalDateTime(column(row, "RECEIVED_AT"))
            ));
        }
        return new PageResponse<>(records, total, safePage, safeSize);
    }

    private String normalizeFolder(String folder) {
        return folder == null || folder.isBlank() ? null : folder.trim().toUpperCase(Locale.ROOT);
    }

    private String snippet(String body, String keyword) {
        if (body == null) return "";
        int idx = body.toLowerCase().indexOf(keyword.toLowerCase());
        if (idx < 0) {
            return body.length() > 100 ? body.substring(0, 100) + "..." : body;
        }
        int start = Math.max(0, idx - 40);
        int end = Math.min(body.length(), idx + keyword.length() + 40);
        String snip = body.substring(start, end);
        if (start > 0) snip = "..." + snip;
        if (end < body.length()) snip = snip + "...";
        return snip;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        return ((Number) val).longValue();
    }

    private Object column(Map<String, Object> row, String name) {
        if (row.containsKey(name)) {
            return row.get(name);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Boolean toBoolean(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return ((Number) val).intValue() != 0;
    }

    private java.time.LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof java.time.LocalDateTime) return (java.time.LocalDateTime) val;
        if (val instanceof java.sql.Timestamp) return ((java.sql.Timestamp) val).toLocalDateTime();
        return null;
    }
}
