package net.mtgsaber.lib.httpserver;

class VolatileWrapper<T> {
    public volatile T val;

    public VolatileWrapper(T val) {
        this.val = val;
    }
}
