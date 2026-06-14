package com.smartmail.attachment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.attachment.entity.PendingAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PendingAttachmentMapper extends BaseMapper<PendingAttachment> {

    @Select("""
            SELECT * FROM pending_attachment
            WHERE id = #{id}
              AND uploader_id = #{uploaderId}
              AND status = 'UPLOADED'
            """)
    PendingAttachment findOwnedPending(Long id, Long uploaderId);

    @Select("""
            SELECT * FROM pending_attachment
            WHERE uploader_id = #{uploaderId}
              AND status = 'UPLOADED'
            ORDER BY created_at DESC
            """)
    List<PendingAttachment> listByUploader(Long uploaderId);
}
