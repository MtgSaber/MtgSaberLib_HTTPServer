package net.mtgsaber.lib.httpserver.util;

import com.sun.net.httpserver.HttpExchange;
import net.mtgsaber.lib.httpserver.HTTPCode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HTTPServerUtils {
    private static final Set<Class<?>> INTERFACES = new HashSet<>();

    public static final int IO_BUFF_SIZE = 1024;

    public static InputStream str2input(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The suppliers are called in this order: code, length, response
     *
     * @param response can be called to obtain the response, which is assumed to do the actual processing.
     * @param length   should reflect the length of the above response
     * @param code     should indicate the status of the HTTP response.
     * @param handler  accepts any and all exceptions produced by exchange I/O.
     * @return a consumer which can be called at a later time to apply the behavior of this function.
     */
    public static Consumer<HttpExchange> process(
            Supplier<InputStream> response, Supplier<Integer> length, Supplier<HTTPCode> code,
            Consumer<Exception> handler
    ) {
        final Consumer<Exception> finHandler = handler != null ? handler : Exception::printStackTrace;
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
}
