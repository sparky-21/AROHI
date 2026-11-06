package com.arohi.healthcare.repositories;

/**
 * Asynchronous repository callback contract for authentication and Firestore operations.
 */
public interface AuthCallback<T> {
    void onSuccess(T result);
    void onError(String errorMessage);
}
