package net.mtgsaber.lib.httpserver;

import com.sun.net.httpserver.HttpExchange;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Processor implements Comparable<Processor>, Closeable {
    protected static final Map<Class<? extends Processor>, Supplier<? extends Processor>> CHILD_CONS_MAP = new HashMap<>();
    protected static final Map<Class<? extends Processor>, Function<String, Boolean>> CHILD_MATCHER_MAP = new HashMap<>();

    protected final Function<String, Boolean> MATCHER;

    protected volatile int load = 0;

    protected Processor(Function<String, Boolean> matcher) {
        this.MATCHER = matcher;
    }

    public void doGET(HttpExchange exchange) {
        // TODO: add default return of "method not supported"
    }

    public void doPOST(HttpExchange exchange) {
        // TODO: add default return of "method not supported"
    }

    public void doPUT(HttpExchange exchange) {
        // TODO: add default return of "method not supported"
    }

    public void doDelete(HttpExchange exchange) {
        // TODO: add default return of "method not supported"
    }

    public void doOptions(HttpExchange exchange) {
        /*
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization, Access-Control-Allow-Origin");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,HEAD");
        //exchange.getResponseHeaders().set("Content-Type", "application/json");

         */
        try {
            System.out.println("Sent options!");
            exchange.sendResponseHeaders(200, -1);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static void addCORSHeaders(HttpExchange exchange) {
        System.out.println("adding cors headers.");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization, Access-Control-Allow-Origin");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,HEAD");
    }

    protected void sendNotFound(HttpExchange exchange) {
        // TODO: send response for resource not found (code 404)
    }

    public abstract void close() throws IOException;

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
            System.err.println("factory or matcher is null!\n\tfactory: " + factory + "\n\tmatcher: " + matcher);
            System.err.println(CHILD_CONS_MAP.toString());
            System.err.println(CHILD_MATCHER_MAP.toString());
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
