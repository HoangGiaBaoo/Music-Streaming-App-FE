package com.example.musicstreamingapp.data;

import androidx.annotation.Nullable;

public class Resource<T> {

    public enum Status { LOADING, SUCCESS, ERROR }

    public final Status status;
    @Nullable public final T data;
    @Nullable public final String message;

    private Resource(Status status, @Nullable T data, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> Resource<T> loading(@Nullable T cached) {
        return new Resource<>(Status.LOADING, cached, null);
    }

    public static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    public static <T> Resource<T> error(String message, @Nullable T cached) {
        return new Resource<>(Status.ERROR, cached, message);
    }

    public boolean isLoading() { return status == Status.LOADING; }
    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isError()   { return status == Status.ERROR; }
}
