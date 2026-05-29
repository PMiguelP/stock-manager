package com.pelletsfactory.stock_manager.desktop.services;

public interface ViewStateful<T> {
    T captureViewState();

    void restoreViewState(T state);
}

