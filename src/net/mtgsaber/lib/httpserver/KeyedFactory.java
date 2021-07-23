package net.mtgsaber.lib.httpserver;

import java.util.function.Function;

interface KeyedFactory<T> {
    T create();

    /**
     * Should be consistent and unique for each implementing class!
     */
    Function<String, Boolean> key();
}
