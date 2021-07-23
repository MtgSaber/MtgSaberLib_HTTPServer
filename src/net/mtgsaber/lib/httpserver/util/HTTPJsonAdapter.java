package net.mtgsaber.lib.httpserver.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import net.mtgsaber.lib.httpserver.HTTPCode;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record HTTPJsonAdapter(Gson gson) {
    public static class Factory {
        private final GsonBuilder gsonBuilder = new GsonBuilder();

        public <T> Factory registerInterface(Class<T> clazz) {
            gsonBuilder.registerTypeAdapter(clazz, new InterfaceAdapter<>());
            return this;
        }
        public Factory setPrettyPrinting() {
            gsonBuilder.setPrettyPrinting(); return this;
        }
        public HTTPJsonAdapter create() {
            return new HTTPJsonAdapter(gsonBuilder.create());
        }
    }

    public <T> T objectifyJsonPayload(HttpExchange exchange, Class<T> clazz, Consumer<Exception> handler) {
        T payload = null;
        try (InputStreamReader input = new InputStreamReader(exchange.getRequestBody())) {
            try {
                payload = gson.fromJson(input, clazz);
            } catch (Exception e) {
                handler.accept(e);
            }
        } catch (IOException e) {
            handler.accept(e);
        }
        return payload;
    }

    public <T> Consumer<HttpExchange> sendPayloadAsJson(
            Supplier<T> response, Supplier<HTTPCode> code,
            Class<T> clazz, Consumer<Exception> handler
    ) {
        String json = gson.toJson(response.get(), clazz);
        return HTTPServerUtils.process(
                () -> HTTPServerUtils.str2input(json), json::length, code, handler
        );
    }
}
