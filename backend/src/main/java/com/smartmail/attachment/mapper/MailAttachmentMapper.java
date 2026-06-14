package com.smartmail.attachment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.attachment.entity.MailAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MailAttachmentMapper extends BaseMapper<MailAttachment> {
    @Select("""
            SELECT * FROM mail_attachment
            WHERE mail_id = #{mailId}
            ORDER BY id ASC
            """)
    List<MailAttachment> listByMailId(Long mailId);
}
