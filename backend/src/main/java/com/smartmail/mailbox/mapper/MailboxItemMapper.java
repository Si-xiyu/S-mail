package com.smartmail.mailbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.mailbox.entity.MailboxItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MailboxItemMapper extends BaseMapper<MailboxItem> {
    @Select("""
            SELECT * FROM mailbox_item
            WHERE user_id = #{userId}
              AND mail_id = #{mailId}
              AND deleted_flag = FALSE
            LIMIT 1
            """)
    MailboxItem findVisibleByUserAndMail(Long userId, Long mailId);

    @Select("""
            SELECT * FROM mailbox_item
            WHERE user_id = #{userId}
              AND folder = #{folder}
              AND deleted_flag = FALSE
            ORDER BY received_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<MailboxItem> listByFolder(Long userId, String folder, long limit, long offset);

    @Select("""
            SELECT COUNT(*)
            FROM mailbox_item
            WHERE user_id = #{userId}
              AND folder = #{folder}
              AND deleted_flag = FALSE
            """)
    long countByFolder(Long userId, String folder);
}
