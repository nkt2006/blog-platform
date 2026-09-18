package ru.mirea.blogplatform.exception;

public class EntityNotFoundException extends BlogPlatformException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
