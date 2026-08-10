package com.nhnacademy.userauthapi.exception;

public class LoginFailException extends RuntimeException{
    public LoginFailException(String message){
        super(message);
    }

}
