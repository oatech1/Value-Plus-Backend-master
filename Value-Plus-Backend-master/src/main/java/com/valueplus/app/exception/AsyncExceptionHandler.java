package com.valueplus.app.exception;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        System.out.println("Exception while executing with message {} "+ throwable.getMessage());
        System.out.println("Exception happen in {} method "+ method.getName());
    }

}
