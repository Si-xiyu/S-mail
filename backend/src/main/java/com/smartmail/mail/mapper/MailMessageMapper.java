package com.smartmail.mail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.mail.entity.MailMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MailMessageMapper extends BaseMapper<MailMessage> {
}
