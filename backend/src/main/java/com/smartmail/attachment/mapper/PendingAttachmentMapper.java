package com.smartmail.attachment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.attachment.entity.PendingAttachment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PendingAttachmentMapper extends BaseMapper<PendingAttachment> {
}
