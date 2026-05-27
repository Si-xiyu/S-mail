package com.smartmail.mail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.mail.entity.MailRecipient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MailRecipientMapper extends BaseMapper<MailRecipient> {
}
