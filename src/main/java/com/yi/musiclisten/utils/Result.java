package com.yi.musiclisten.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.exception.ParamCheckException;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结果
 * @param <T>
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    private int code;
    private String message;
    private T data;
    /**
     * 无数据的返回信息
     * @param responseEnum
     */
    public Result(ResponseEnum responseEnum) {
        this.code = responseEnum.getCode();
        this.message = responseEnum.getMsg();
    }

    /**
     * 带数据的返回信息
     * @param responseEnum
     * @param data
     */
    public Result(ResponseEnum responseEnum, T data) {
        this.code = responseEnum.getCode();
        this.message = responseEnum.getMsg();
        this.data = data;
    }

    public Result(int code, String msg) {
    this.code = code;
        this.message = msg;
    }


    /**
     * 成功返回带数据
     * @param data
     * @return
     * @param <T>
     */
    public  static <T> Result<T> success(ResponseEnum responseEnum,T data){
        return new Result<T>(ResponseEnum.SUCCESS,data);
    }

    /**
     * 成功返回不带数据
     * @return
     * @param <T>
     */
    public static <T> Result<T> success(ResponseEnum responseEnum){
        return new Result<T>(ResponseEnum.SUCCESS,null);
    }

    /**
     * 失败返回不带数据
     * @param responseEnum
     * @return
     * @param <T>
     */
    public static  <T> Result<T> fail(ResponseEnum responseEnum){
        return new Result<T>(responseEnum,null);
    }

    /**
     * 自定义失败错误信息
     * @param code
     * @param msg
     * @return
     * @param <T>
     */
    public static  <T> Result<T> fail(int code,String msg){
        return new Result<T>(code,msg);
    }
    /**
     * 参数校验，如果校验失败返回 Result.fail，否则返回 null
     * @param condition 校验条件，true表示校验失败
     * @param message 提示信息
     */
    public static void checkParam(boolean condition, String message) {
        if (condition) {
            throw new ParamCheckException(message);
        }
    }
    /**
     * 失败返回带数据
     * @param responseEnum
     * @param data
     * @return
     * @param <T>
     */
    public static  <T> Result<T> fail(ResponseEnum responseEnum,T data){
        return new Result<T>(responseEnum,data);
    }

}
