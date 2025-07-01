package com.dreamfish.backend.service;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 邮件服务
 * @date 2025/4/11 12:10
 */
public interface EmailService {
    /**
     * 发送邮件
     *
     * @param name    发送人姓名
     * @param from    发送人邮箱
     * @param to      收件人邮箱
     * @param subject 主题
     * @param content 内容
     * @param isHtml  是否是html
     */
    void send(String name, String from, String to, String subject, String content, Boolean isHtml);

    /**
     * 发送验证码
     *
     * @param to   收件人邮箱
     * @param code 验证码
     */
    void sendVerifyCode(String to, String code);

    /**
     * 异步发送验证码(异步)
     *
     * @param to   收件人邮箱
     * @param code 验证码
     */
    void asyncSendVerifyCode(String to, String code);

    /**
     * 发送任务通知(异步)
     *
     * @param to          收件人邮箱
     * @param htmlContent html内容
     */
    void sendTask(String to, String htmlContent);

    /**
     * 发送简单邮件(异步)
     *
     * @param to      收件人邮箱
     * @param content 内容
     * @param subject 主题
     */
    void sendMessage(String to, String content, String subject);
}
