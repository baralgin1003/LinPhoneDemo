package com.example.administrator.linphonedemo.linphone;

public interface DataFetcher<T> {
    void onSuccess(T t);
    void onException(Throwable throwable);
}
