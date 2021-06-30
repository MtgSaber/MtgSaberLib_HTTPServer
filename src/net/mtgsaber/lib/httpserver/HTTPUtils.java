package net.mtgsaber.lib.httpserver;

import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class HTTPUtils {
    public static final int IO_BUFF_SIZE = 1024;

    public static Consumer<HttpExchange> process(
            HTTPCode code,
            Supplier<Integer> length,
            Supplier<InputStream> response,
            Consumer<Exception> handler
    ) {
        final Consumer<Exception> finHandler = handler != null? handler : Exception::printStackTrace;
        return exchange -> {
            try {
                int len = length.get();
                exchange.sendResponseHeaders(code.code(), len);
                if (len != 0) {
                    try (
                            OutputStream out = exchange.getResponseBody();
                            InputStream in = response.get()
                    ) {
                        final byte[] buffer = new byte[IO_BUFF_SIZE];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        out.flush();
                    }
                }
            } catch (IOException e) {
                finHandler.accept(e);
            }
        };
    }
}
