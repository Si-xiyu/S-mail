package com.smartmail.mailbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.mailbox.entity.MailboxItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @Select("""
            SELECT COUNT(*)
            FROM mailbox_item
            WHERE user_id = #{userId}
              AND deleted_flag = FALSE
              AND read_flag = FALSE
              AND folder IN ('INBOX', 'JUNK')
            """)
    long countUnread(Long userId);

    @Select("""
            SELECT * FROM mailbox_item
            WHERE user_id = #{userId}
              AND deleted_flag = FALSE
            ORDER BY received_at DESC
            """)
    List<MailboxItem> listVisibleByUser(Long userId);

    @Select("""
            <script>
            SELECT mi.*, mm.sender_email, mm.subject, mm.content_text, mm.has_attachment
            FROM mailbox_item mi
            JOIN mail_message mm ON mi.mail_id = mm.id
            WHERE mi.user_id = #{userId}
              AND mi.deleted_flag = FALSE
              AND (mm.subject LIKE #{keyword}
                   OR mm.sender_email LIKE #{keyword}
                   OR mm.content_text LIKE #{keyword}
                   OR EXISTS (
                       SELECT 1
                       FROM mail_category_assignment mca
                       JOIN mail_category mc ON mc.id = mca.category_id
                       WHERE mca.user_id = mi.user_id
                         AND mca.mail_id = mi.mail_id
                         AND mc.name LIKE #{keyword}
                   ))
              <if test="folder != null">
              AND mi.folder = #{folder}
              </if>
              <if test="categoryId != null">
              AND EXISTS (
                  SELECT 1 FROM mail_category_assignment mca
                  WHERE mca.user_id = mi.user_id
                    AND mca.mail_id = mi.mail_id
                    AND mca.category_id = #{categoryId}
              )
              </if>
              <if test="starred != null and starred">
              AND mi.star_flag = TRUE
              </if>
            ORDER BY mi.received_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Map<String, Object>> searchByKeyword(@Param("userId") Long userId,
                                              @Param("keyword") String keyword,
                                              @Param("folder") String folder,
                                              @Param("categoryId") Long categoryId,
                                              @Param("starred") Boolean starred,
                                              @Param("limit") long limit,
                                              @Param("offset") long offset);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM mailbox_item mi
            JOIN mail_message mm ON mi.mail_id = mm.id
            WHERE mi.user_id = #{userId}
              AND mi.deleted_flag = FALSE
              AND (mm.subject LIKE #{keyword}
                   OR mm.sender_email LIKE #{keyword}
                   OR mm.content_text LIKE #{keyword}
                   OR EXISTS (
                       SELECT 1
                       FROM mail_category_assignment mca
                       JOIN mail_category mc ON mc.id = mca.category_id
                       WHERE mca.user_id = mi.user_id
                         AND mca.mail_id = mi.mail_id
                         AND mc.name LIKE #{keyword}
                   ))
              <if test="folder != null">
              AND mi.folder = #{folder}
              </if>
              <if test="categoryId != null">
              AND EXISTS (
                  SELECT 1 FROM mail_category_assignment mca
                  WHERE mca.user_id = mi.user_id
                    AND mca.mail_id = mi.mail_id
                    AND mca.category_id = #{categoryId}
              )
              </if>
              <if test="starred != null and starred">
              AND mi.star_flag = TRUE
              </if>
            </script>
            """)
    long countByKeyword(@Param("userId") Long userId,
                        @Param("keyword") String keyword,
                        @Param("folder") String folder,
                        @Param("categoryId") Long categoryId,
                        @Param("starred") Boolean starred);

    @Select("""
            SELECT * FROM mailbox_item
            WHERE user_id = #{userId}
              AND folder = 'INBOX'
              AND deleted_flag = FALSE
              AND read_flag = FALSE
            ORDER BY received_at DESC
            """)
    List<MailboxItem> listUnread(Long userId);

    @Select("""
            SELECT * FROM mailbox_item
            WHERE user_id = #{userId}
              AND folder = 'INBOX'
              AND deleted_flag = FALSE
              AND priority IN ('HIGH', 'URGENT')
            ORDER BY received_at DESC
            """)
    List<MailboxItem> listImportant(Long userId);

    @Select("""
            SELECT * FROM mailbox_item
            WHERE user_id = #{userId}
              AND deleted_flag = FALSE
              AND received_at >= #{startOfDay}
            ORDER BY received_at DESC
            """)
    List<MailboxItem> listToday(Long userId, LocalDateTime startOfDay);

    @Select("""
            SELECT COUNT(*) FROM mailbox_item
            WHERE user_id = #{userId}
              AND folder = #{folder}
              AND deleted_flag = FALSE
              AND read_flag = FALSE
            """)
    long countUnreadByFolder(Long userId, String folder);

    @Select("""
            SELECT COUNT(*) FROM mailbox_item
            WHERE user_id = #{userId}
              AND folder = 'INBOX'
              AND deleted_flag = FALSE
              AND priority IN ('HIGH', 'URGENT')
            """)
    long countImportant(Long userId);

    @Select("""
            SELECT COUNT(*) FROM mailbox_item
            WHERE user_id = #{userId}
              AND deleted_flag = FALSE
              AND received_at >= #{startOfDay}
            """)
    long countToday(Long userId, LocalDateTime startOfDay);

    @Select("""
            SELECT COUNT(*) FROM mailbox_item
            WHERE user_id = #{userId}
              AND deleted_flag = FALSE
              AND folder IN ('INBOX', 'JUNK')
              AND received_at > #{since}
              AND received_at <= #{until}
            """)
    long countSince(Long userId, LocalDateTime since, LocalDateTime until);

    @Select("""
            <script>
            SELECT * FROM mailbox_item
            WHERE user_id = #{userId}
              AND deleted_flag = FALSE
              AND mail_id IN
              <foreach item="mailId" collection="mailIds" open="(" separator="," close=")">
                #{mailId}
              </foreach>
            ORDER BY received_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<MailboxItem> listByMailIds(@Param("userId") Long userId,
                                     @Param("mailIds") List<Long> mailIds,
                                     @Param("limit") long limit,
                                     @Param("offset") long offset);

    @Select("""
            <script>
            SELECT COUNT(*) FROM mailbox_item
            WHERE user_id = #{userId}
              AND deleted_flag = FALSE
              AND mail_id IN
              <foreach item="mailId" collection="mailIds" open="(" separator="," close=")">
                #{mailId}
              </foreach>
            </script>
            """)
    long countByMailIds(@Param("userId") Long userId, @Param("mailIds") List<Long> mailIds);
}
