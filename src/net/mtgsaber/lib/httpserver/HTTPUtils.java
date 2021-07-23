package net.mtgsaber.lib.httpserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HTTPUtils {
    private static final Set<Class<?>> INTERFACES = new HashSet<>();

    public static final int IO_BUFF_SIZE = 1024;
    public static volatile Gson GSON = new GsonBuilder().create();
    public static volatile Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    private static void setGsonInstances() {
        GsonBuilder builder = new GsonBuilder();
        INTERFACES.forEach(clazz -> builder.registerTypeAdapter(clazz, new InterfaceAdapter<>()));
    }

    /**
     * The suppliers are called in this order: code, length, response
     * @param response can be called to obtain the response, which is assumed to do the actual processing.
     * @param length should reflect the length of the above response
     * @param code should indicate the status of the HTTP response.
     * @param handler accepts any and all exceptions produced by exchange I/O.
     * @return a consumer which can be called at a later time to apply the behavior of this function.
     */
    public static Consumer<HttpExchange> process(
            Supplier<InputStream> response, Supplier<Integer> length, Supplier<HTTPCode> code,
            Consumer<Exception> handler
    ) {
        final Consumer<Exception> finHandler = handler != null? handler : Exception::printStackTrace;
        return exchange -> {
            try (
                    OutputStream out = exchange.getResponseBody();
                    InputStream in = response.get()
            ) {
                int len = length.get();
                exchange.sendResponseHeaders(code.get().code(), len);
                if (len >= 0) {
                    final byte[] buffer = new byte[IO_BUFF_SIZE];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                out.flush();
            } catch (IOException e) {
                finHandler.accept(e);
            }
        };
    }

    public static <T> T objectifyJsonPayload(HttpExchange exchange, Class<T> clazz, Consumer<Exception> handler) {
        T payload = null;
        try (InputStreamReader input = new InputStreamReader(exchange.getRequestBody())) {
            try {
                payload = GSON.fromJson(input, clazz);
            } catch (Exception e) {
                handler.accept(e);
            }
        } catch (IOException e) {
            handler.accept(e);
        }
        return payload;
    }

    public static <T> Consumer<HttpExchange> sendPayloadAsJson(
            Supplier<T> response, Supplier<HTTPCode> code,
            Class<T> clazz, Consumer<Exception> handler
    ) {
        String json = GSON.toJson(response.get(), clazz);
        return process(
                () -> str2input(json), json::length, code, handler
        );
    }

    public static InputStream str2input(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
