package org.ikuzo.otboo.global.util;

public interface EmailService {
    void sendTemporaryPassword(String email, String name, String tempPassword);
}