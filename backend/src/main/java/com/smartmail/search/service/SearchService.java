package com.smartmail.search.service;

import com.smartmail.common.response.PageResponse;
import com.smartmail.common.security.UserContext;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import com.smartmail.search.dto.SearchResultResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final MailboxItemMapper mailboxItemMapper;

    public SearchService(MailboxItemMapper mailboxItemMapper) {
        this.mailboxItemMapper = mailboxItemMapper;
    }

    public PageResponse<SearchResultResponse> search(String keyword, String folder, long page, long pageSize) {
        Long userId = UserContext.requireUserId();
        String likeKeyword = "%" + keyword.trim() + "%";
        long offset = (page - 1) * pageSize;

        List<Map<String, Object>> rows = mailboxItemMapper.searchByKeyword(
                userId, likeKeyword, folder != null ? folder : null, pageSize, offset);
        long total = mailboxItemMapper.countByKeyword(userId, likeKeyword,
                folder != null ? folder : null);

        List<SearchResultResponse> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            records.add(new SearchResultResponse(
                    toLong(row.get("ID")),
                    toLong(row.get("MAIL_ID")),
                    (String) row.get("SENDER_EMAIL"),
                    (String) row.get("SUBJECT"),
                    snippet((String) row.get("CONTENT_TEXT"), keyword),
                    (String) row.get("FOLDER"),
                    toBoolean(row.get("READ_FLAG")),
                    toBoolean(row.get("STAR_FLAG")),
                    (String) row.get("PRIORITY"),
                    toBoolean(row.get("HAS_ATTACHMENT")),
                    toLocalDateTime(row.get("RECEIVED_AT"))
            ));
        }
        return new PageResponse<>(records, total, page, pageSize);
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
