package com.yi.musiclisten.exception;

import com.yi.musiclisten.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ParamCheckException.class)
    public Result<?> handleParamCheckException(ParamCheckException ex, HttpServletRequest request) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleOtherException(Exception ex) {
        return Result.fail(500, ex.getMessage());
    }
}
