package com.example.musicstreamingapp.data;

import androidx.annotation.Nullable;

import java.util.function.Consumer;

public class Event<T> {

    private final T content;
    private boolean handled = false;

    public Event(T content) {
        this.content = content;
    }

    @Nullable
    public T getIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }

    public T peek() { return content; }

    public void consume(Consumer<T> action) {
        T value = getIfNotHandled();
        if (value != null) action.accept(value);
    }
}
