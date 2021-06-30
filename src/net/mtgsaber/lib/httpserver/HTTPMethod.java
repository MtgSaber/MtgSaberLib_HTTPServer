package net.mtgsaber.lib.httpserver;

import java.util.HashMap;
import java.util.Map;

public interface HTTPMethod {
    String name();

    enum Standard implements HTTPMethod {
        GET,
        POST,
        PUT,
        DELETE,
        OPTIONS,
        ;

        private static final Map<String, Standard> STRING_LOOKUP_MAP = new HashMap<>();
        private static volatile boolean IS_INIT_STRING_LOOKUP_MAP = false;

        public static Standard fromString(String name) {
            synchronized (STRING_LOOKUP_MAP) {
                if (!IS_INIT_STRING_LOOKUP_MAP) {
                    for (Standard method : values())
                        STRING_LOOKUP_MAP.put(method.name(), method);
                    IS_INIT_STRING_LOOKUP_MAP = true;
                }
                return STRING_LOOKUP_MAP.get(name);
            }
        }
    }
}
