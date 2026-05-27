package com.smartmail.internal.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartmail.ai.entity.MailAiResult;
import com.smartmail.ai.mapper.MailAiResultMapper;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.internal.dto.InternalMailResponse;
import com.smartmail.internal.dto.SaveAiResultRequest;
import com.smartmail.internal.dto.SetPriorityRequest;
import com.smartmail.mail.entity.MailMessage;
import com.smartmail.mail.entity.MailRecipient;
import com.smartmail.mail.mapper.MailMessageMapper;
import com.smartmail.mail.mapper.MailRecipientMapper;
import com.smartmail.mailbox.entity.MailboxItem;
import com.smartmail.mailbox.mapper.MailboxItemMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InternalToolService {
    private final MailMessageMapper mailMapper;
    private final MailRecipientMapper recipientMapper;
    private final MailboxItemMapper mailboxMapper;
    private final MailAiResultMapper aiResultMapper;

    public InternalToolService(
            MailMessageMapper mailMapper,
            MailRecipientMapper recipientMapper,
            MailboxItemMapper mailboxMapper,
            MailAiResultMapper aiResultMapper
    ) {
        this.mailMapper = mailMapper;
        this.recipientMapper = recipientMapper;
        this.mailboxMapper = mailboxMapper;
        this.aiResultMapper = aiResultMapper;
    }

    public InternalMailResponse getMail(Long mailId, Long userId) {
        MailboxItem item = mailboxMapper.findVisibleByUserAndMail(userId, mailId);
        if (item == null) {
            throw new BusinessException(404, "邮件不存在或用户无权访问");
        }
        MailMessage mail = mailMapper.selectById(mailId);
        List<String> recipients = recipientMapper.selectList(new QueryWrapper<MailRecipient>().eq("mail_id", mailId))
                .stream()
                .map(MailRecipient::getRecipientEmail)
                .toList();
        return new InternalMailResponse(
                mailId,
                userId,
                mail.getSenderEmail(),
                mail.getSubject(),
                mail.getContentText(),
                mail.getContentHtml(),
                item.getPriority(),
                recipients
        );
    }

    public void saveAiResult(SaveAiResultRequest request) {
        MailAiResult result = new MailAiResult();
        result.setMailId(request.mailId());
        result.setUserId(request.userId());
        result.setResultType(request.resultType());
        result.setResultJson(request.resultJson());
        result.setStatus(request.status());
        result.setCreatedAt(LocalDateTime.now());
        result.setUpdatedAt(LocalDateTime.now());
        aiResultMapper.insert(result);
    }

    public void setPriority(Long mailId, SetPriorityRequest request) {
        MailboxItem item = mailboxMapper.findVisibleByUserAndMail(request.userId(), mailId);
        if (item == null) {
            throw new BusinessException(404, "邮箱条目不存在");
        }
        item.setPriority(request.priority().trim().toUpperCase());
        item.setUpdatedAt(LocalDateTime.now());
        mailboxMapper.updateById(item);
    }
}
