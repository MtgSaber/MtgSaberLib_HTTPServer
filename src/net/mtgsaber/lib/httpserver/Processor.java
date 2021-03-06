package net.mtgsaber.lib.httpserver;

import com.sun.net.httpserver.HttpExchange;
import net.mtgsaber.lib.httpserver.util.HTTPJsonAdapter;
import net.mtgsaber.lib.httpserver.util.HTTPServerUtils;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Processor implements Comparable<Processor>, Closeable {
    protected static final Map<Class<? extends Processor>, Supplier<? extends Processor>> CHILD_CONS_MAP = new HashMap<>();
    protected static final Map<Class<? extends Processor>, Function<String, Boolean>> CHILD_MATCHER_MAP = new HashMap<>();

    protected final Function<String, Boolean> MATCHER;
    protected final HTTPCode DEFAULT_RESPONSE_CODE;
    protected final HTTPJsonAdapter JSON_ADAPTER;

    protected volatile int load = 0;

    protected Processor(Function<String, Boolean> matcher) {
        this(matcher, new HTTPJsonAdapter.Factory());
    }

    protected Processor(Function<String, Boolean> matcher, HTTPJsonAdapter.Factory jsonAdapterFactory) {
        this(matcher, jsonAdapterFactory, HTTPCode.IANACode.MethodNotAllowed);
    }

    protected Processor(Function<String, Boolean> matcher, HTTPCode dfault) {
        this(matcher, new HTTPJsonAdapter.Factory(), dfault);
    }

    protected Processor(
            Function<String, Boolean> matcher, HTTPJsonAdapter.Factory jsonAdapterFactory, HTTPCode dfault
    ) {
        this.JSON_ADAPTER = jsonAdapterFactory.create();
        this.MATCHER = matcher;
        this.DEFAULT_RESPONSE_CODE = dfault;
    }

    public void doGET(HttpExchange exchange) {
        HTTPServerUtils.process(()->null, ()->-1, ()->DEFAULT_RESPONSE_CODE, null).accept(exchange);
    }

    public void doPOST(HttpExchange exchange) {
        HTTPServerUtils.process(()->null, ()->-1, ()->DEFAULT_RESPONSE_CODE, null).accept(exchange);
    }

    public void doPUT(HttpExchange exchange) {
        HTTPServerUtils.process(()->null, ()->-1, ()->DEFAULT_RESPONSE_CODE, null).accept(exchange);
    }

    public void doDelete(HttpExchange exchange) {
        HTTPServerUtils.process(()->null, ()->-1, ()->DEFAULT_RESPONSE_CODE, null).accept(exchange);
    }

    public void doOptions(HttpExchange exchange) {
        HTTPServerUtils.process(()->null, ()->-1, ()->HTTPCode.IANACode.NoContent, null).accept(exchange);
    }

    /**
     * This can be used in later versions to support multiple handler instances for efficiency.
     * @param o
     * @return
     */
    @Override
    public int compareTo(Processor o) {
        return 0;
    }

    public static <C extends Processor> KeyedFactory<C> factory(Class<C> clazz) {
        try {
            Class.forName(clazz.getName());
        } catch (ClassNotFoundException cnfex) {
            cnfex.printStackTrace();
            return null;
        }
        Supplier<C> factory = null;
        Function<String, Boolean> matcher = null;
        try {
            factory = ((Supplier<C>) CHILD_CONS_MAP.get(clazz));
            matcher = CHILD_MATCHER_MAP.get(clazz);
        } catch (ClassCastException ccex) {
            ccex.printStackTrace();
        }
        if (factory == null || matcher == null) {
            return null;
        }
        final Supplier<C> finFactory = factory;
        final Function<String, Boolean> finMatcher = matcher;
        return new KeyedFactory<>() {
            @Override
            public C create() {
                return finFactory.get();
            }

            @Override
            public Function<String, Boolean> key() {
                return finMatcher;
            }
        };
    }
}
