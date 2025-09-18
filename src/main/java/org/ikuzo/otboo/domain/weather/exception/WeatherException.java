package org.ikuzo.otboo.domain.weather.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;
import org.ikuzo.otboo.global.exception.OtbooException;

public class WeatherException extends OtbooException {
    public WeatherException(ErrorCode errorCode) {
        super(errorCode);
    }

    public WeatherException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}