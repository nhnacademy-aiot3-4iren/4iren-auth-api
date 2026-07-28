package com.nhnacademy.userauthapi.exception;

//RuntimeException : 실행 시 발생하는 예외
public class LoginFailException extends RuntimeException{
    public LoginFailException(String message){
        super(message);
    }

}
