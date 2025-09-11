package org.ikuzo.otboo.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {;


  private final String message;

  ErrorCode(String message) {
    this.message = message;
  }
} 