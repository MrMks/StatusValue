package com.github.mrmks.status;

public class BrokenImplementationException extends Exception {
    BrokenImplementationException() {
        super();
    }

    BrokenImplementationException(String msg) {
        super(msg);
    }

    BrokenImplementationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
