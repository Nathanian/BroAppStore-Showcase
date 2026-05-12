package com.example.appstoredemo.utils;

import androidx.annotation.Nullable;

/**
 * Simple result wrapper representing either a value or an error message.
 */
public class Result<T> {
    @Nullable
    private final T data;
    @Nullable
    private final String error;

    private Result(@Nullable T data, @Nullable String error) {
        this.data = data;
        this.error = error;
    }

    /**
     * Creates a successful result wrapper.
     *
     * @param <T> type of the wrapped value.
     * @param data payload to store.
     * @return result instance containing the provided data.
     * @throws IllegalStateException if {@code data} is {@code null}.
     */
    public static <T> Result<T> success(T data) {
        if (data == null) {
            throw new IllegalStateException("Successful result requires non-null data");
        }
        return new Result<>(data, null);
    }

    /**
     * Creates an error result wrapper.
     *
     * @param <T> type of the result payload.
     * @param message error description.
     * @return result instance containing the error.
     * @throws IllegalArgumentException if {@code message} is {@code null} or empty.
     */
    public static <T> Result<T> error(String message) {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Error message must not be empty");
        }
        return new Result<>(null, message);
    }

    /**
     * Indicates whether the result represents success.
     *
     * @return {@code true} when an error is not present.
     * @throws IllegalStateException if both data and error are {@code null}.
     */
    public boolean isSuccess() {
        if (data == null && error == null) {
            throw new IllegalStateException("Result state undefined");
        }
        return error == null;
    }

    @Nullable
    /**
     * Returns the wrapped data when available.
     *
     * @return data value or {@code null} if this result is an error.
     * @throws IllegalStateException if both data and error are {@code null}.
     */
    public T getData() {
        if (data == null && error == null) {
            throw new IllegalStateException("Result state undefined");
        }
        return data;
    }

    @Nullable
    /**
     * Returns the error message when available.
     *
     * @return error message or {@code null} for successful results.
     * @throws IllegalStateException if both data and error are {@code null}.
     */
    public String getError() {
        if (data == null && error == null) {
            throw new IllegalStateException("Result state undefined");
        }
        return error;
    }
}
