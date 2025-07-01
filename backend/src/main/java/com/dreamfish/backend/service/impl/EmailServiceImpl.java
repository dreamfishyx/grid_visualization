package com.dreamfish.backend.service.impl;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.exception.BusinessException;
import com.dreamfish.backend.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/4/11 12:11
 */
@Service
@EnableScheduling
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String from;
    @Value("${spring.mail.nickname}")
    private String name;

    @Value("${verification.code.expiration}")
    private int expiration;

    @Override
    public void send(String name, String from, String to, String subject, String content, Boolean isHtml) {
        if (StringUtils.isAnyBlank(from, to, subject, content)) {
            throw new BusinessException(ErrorCode.EMAIL_INFO_ERROR, "邮件信息不完整");
        }
        try {
            MimeMessageHelper helper = new MimeMessageHelper(javaMailSender.createMimeMessage());
            // 设置发件人
            helper.setFrom(new InternetAddress(name + "<" + from + ">"));
            // 设置收件人
            helper.setTo(to);
            // 设置主题
            helper.setSubject(subject);
            // 设置内容
            helper.setText(content, isHtml);
            //设置发送时间
            helper.setSentDate(new java.util.Date());
            // 发送
            javaMailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void sendVerifyCode(String to, String code) {
        String content = "感谢您使用本系统,您的验证码为: " + code + " ,验证码有效时间为" + expiration + "分钟" +
                ",请不要把验证码泄露给其他人。";
        String subject = "验证码";
        send(name, from, to, subject, content, false);
    }

    @Override
    @Async //异步发送,避免前端等待超时
    public void asyncSendVerifyCode(String email, String code) {
        sendVerifyCode(email, code);
    }

    @Override
    @Async
    public void sendTask(String to, String htmlContent) {
        String subject = "任务通知";
        send(name, from, to, subject, htmlContent, true);
    }

    @Override
    @Async
    public void sendMessage(String to, String content, String subject) {
        send(name, from, to, subject, content, false);

    }
}