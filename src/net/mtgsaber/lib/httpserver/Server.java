package net.mtgsaber.lib.httpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

public class Server implements HttpHandler {
    // the actual url-service mapping & executors
    private final Map<Function<String, Boolean>, KeyedFactory<? extends Processor>> ROUTING_MAP = new HashMap<>();
    private final Map<KeyedFactory<? extends Processor>, PriorityQueue<Processor>> INSTANCE_MAP = new HashMap<>();
    private final ExecutorService EXECUTOR; // worker threads to service http connections

    // synchronization & shutdown
    private volatile boolean alive = true; // used to safely shutdown the server
    private final VolatileWrapper<Integer> callCount = new VolatileWrapper<>(0); // acts as an "inverted" semaphore
    private final Object shutdownSleepObj = new Object(); // used for shutdown timing

    // customizable exception handling
    private volatile Consumer<HttpExchange> badPathHandler;

    public Server(ExecutorService executor) {
        this.EXECUTOR = executor;
    }

    public Server setBadPathHandler(Consumer<HttpExchange> badPathHandler) {
        this.badPathHandler = badPathHandler;
        return this;
    }

    /**
     * A streamlined method to register handlers. Exploits static child class registration with base class.
     * If you have an Processor child class you want to register, assuming it has statically
     * registered itself with the base class, you simply call like so:
     * <code>apiServer.registerHandlerClass(MyProcessorClassName.class);</code>
     * and that's it.
     * @param clazz
     * @param <C>
     * @return
     */
    public <C extends Processor> boolean registerHandlerClass(Class<C> clazz) {
        return addHandler(Processor.factory(clazz));
    }

    /**
     * Registers a handler factory for its path.
     * @param handlerFactory
     * @return
     */
    public boolean addHandler(KeyedFactory<? extends Processor> handlerFactory) {
        synchronized (callCount) {
            if (!alive) return false;
            callCount.val++;
        }
        //System.out.println(handlerFactory.matcher());
        try {
            synchronized (ROUTING_MAP) {
                synchronized (INSTANCE_MAP) {
                    if (!ROUTING_MAP.containsKey(handlerFactory.key())) {
                        ROUTING_MAP.put(
                                handlerFactory.key(),
                                handlerFactory
                        );
                        INSTANCE_MAP.put(
                                handlerFactory,
                                new PriorityQueue<>(
                                        (o1, o2) -> o2.load - o1.load // this will order them s.t. lowest load will be at front of queue.
                                )
                        );
                        return true;
                    } else return false;
                }
            }
        } finally {
            decCallCount();
        }
    }

    /**
     * This method routes an exchange to a registered handler, if there is one.
     * If there is no registered handler for the path of this request, error 404 is returned.
     * This can, in future versions, optimize path pattern matching by using a priority queue of patterns based on
     * the number of matches a given pattern has made.
     * @param exchange
     * @throws IOException
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        synchronized (callCount) {
            if (!alive) return;
            callCount.val++;
        }

        try {
            // extract path & method, call corresponding method from appropriate handler. init a new handler if need be.
            final String path = exchange.getRequestURI().getRawPath();
            final HTTPMethod.Standard method = HTTPMethod.Standard.fromString(exchange.getRequestMethod());
            final Processor handler = getHandler(path);
            System.out.println(handler);
            Consumer<HttpExchange> processor = null;
            if (handler != null) {
                switch (method) {
                    case GET -> processor = handler::doGET;
                    case POST -> processor = handler::doPOST;
                    case PUT -> processor = handler::doPUT;
                    case DELETE -> processor = handler::doDelete;
                    case OPTIONS -> processor = handler::doOptions;
                }
            } else
                processor = badPathHandler;
            final Consumer<HttpExchange> finProcessor = processor;
            //System.out.println("processor: " + processor);

            EXECUTOR.execute(() -> {
                if (finProcessor != null) {
                    finProcessor.accept(exchange);
                    //System.out.println("processor accept() finished!");
                }
                else {
                    exchange.close();
                    //System.err.println("Exchange processor was null!");
                }
            });
        } finally {
            decCallCount();
        }
    }

    public Processor getHandler(String path) {
        synchronized (callCount) {
            if (!alive) return null;
            callCount.val++;
        }

        try {
            KeyedFactory<? extends Processor> factory = null;
            Processor handler;

            // TODO: This can be replaced by a more efficient search by using a priority queue.
            Set<Function<String, Boolean>> keySet;
            synchronized (ROUTING_MAP) {
                keySet = ROUTING_MAP.keySet();
            }
            //System.out.println("KeySet for matchers: " + keySet.toString());
            for (Function<String, Boolean> matcher : keySet)
                if (matcher.apply(path)) {
                    synchronized (ROUTING_MAP) {
                        factory = ROUTING_MAP.get(matcher);
                    }
                    break;
                }

            if (factory == null) {
                //System.err.println("getHandler(): factory was null!");
                return null;
            }

            synchronized (INSTANCE_MAP) {
                PriorityQueue<Processor> instanceQueue = INSTANCE_MAP.get(factory);
                if (instanceQueue.isEmpty())
                    instanceQueue.add(factory.create());
                handler = instanceQueue.peek();
            }

            return handler;
        } finally {
            decCallCount();
        }
    }

    private void decCallCount() {
        synchronized (callCount) {
            callCount.val--;
            synchronized (shutdownSleepObj) {shutdownSleepObj.notify();}
        }
    }

    /**
     * Will block until the server is shut down.
     */
    public synchronized void shutdown() {
        if (!alive) return;
        alive = false; // tells other methods to stop servicing new calls.
        while (true) { // wait for current method calls to terminate.
            synchronized (callCount) {
                if (callCount.val == 0) break;
            }
            synchronized (shutdownSleepObj) {
                try {
                    shutdownSleepObj.wait(1000);
                } catch (InterruptedException iex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        EXECUTOR.shutdown();
        // finished waiting, time to close handlers...
        for (PriorityQueue<? extends Processor> handlerQueue : INSTANCE_MAP.values()) {
            for (Processor handler : handlerQueue) {
                try {
                    handler.close();
                } catch (IOException e) {
                    e.printStackTrace(); // TODO: handle ioexceptions!
                }
            }
        }
    }
}
