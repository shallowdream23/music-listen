package com.yi.musiclisten.utils;

import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class EmailCodeUtil {

    @Resource
    private JavaMailSender mailSender;

    /**
     * 生成 6 位数字验证码
     */
    public String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 发送验证码邮件
     *
     * @param toMail 收件人邮箱
     * @param code   验证码
     */
    public void sendCodeEmail(String toMail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("1796340674@qq.com");
            helper.setTo(toMail);
            helper.setSubject("您的验证码");

            String html = "<div style='font-size:16px;'>"
                    + "<p>您好，您的验证码为：</p>"
                    + "<p style='font-size:24px; font-weight:bold;'>" + code + "</p>"
                    + "<p>该验证码 5 分钟内有效，请勿泄露给他人。</p>"
                    + "</div>";

            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}
