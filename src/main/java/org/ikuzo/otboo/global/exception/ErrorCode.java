package org.ikuzo.otboo.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    DUPLICATED_ATTRIBUTE_NAME("이미 존재하는 속성 이름입니다");


  private final String message;

  ErrorCode(String message) {
    this.message = message;
  }
} 