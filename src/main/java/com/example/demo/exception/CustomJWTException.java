package com.example.demo.exception;

public class CustomJWTException extends RuntimeException{
    public CustomJWTException(String msg){
        super(msg);
    }
}
