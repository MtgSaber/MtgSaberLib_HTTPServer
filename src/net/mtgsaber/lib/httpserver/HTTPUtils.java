package net.mtgsaber.lib.httpserver;

import java.util.HashMap;
import java.util.Map;

public class HTTPUtils {
    public enum HTTPCode {
        Continue(100),
        SwitchingProtocols(101),

        OK(200),
        Created(201),
        Accepted(202),
        Non_AuthoritativeInformation(203),
        NoContent(204),
        ResetContent(205),


        ;

        public final int code;

        HTTPCode(int code) {
            this.code = code;
        }
    }
}
