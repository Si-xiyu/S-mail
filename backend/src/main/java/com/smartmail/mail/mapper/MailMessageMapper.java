package com.smartmail.mail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.mail.entity.MailMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MailMessageMapper extends BaseMapper<MailMessage> {
    /**
     * 根据 threadId 获取线程内的所有邮件
     */
    @Select("""
            SELECT * FROM mail_message
            WHERE thread_id = #{threadId}
            ORDER BY sent_at ASC
            """)
    List<MailMessage> findByThreadId(Long threadId);

    /**
     * 根据 parentMailId 获取所有回复
     */
    @Select("""
            SELECT * FROM mail_message
            WHERE parent_mail_id = #{parentMailId}
            ORDER BY sent_at ASC
            """)
    List<MailMessage> findByParentMailId(Long parentMailId);
}

