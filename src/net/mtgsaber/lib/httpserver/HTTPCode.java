package net.mtgsaber.lib.httpserver;

public interface HTTPCode {
    String name();
    int code();
    record NonEnumerated(String name, int code) implements HTTPCode{}
    enum IANACode implements HTTPCode {
        Continue(100),
        SwitchingProtocols(101),

        OK(200),
        Created(201),
        Accepted(202),
        Non_AuthoritativeInformation(203),
        NoContent(204),
        ResetContent(205),

        MultipleChoices(300),
        MovedPermanently(301),
        Found(302),
        SeeOther(303),
        NotModified(304),
        UseProxy(305),
        SwitchProxy(306),
        TemporaryRedirect(307),

        BadRequest(400),
        Unautherized(401),
        PaymentRequired(402),
        Forbidden(403),
        NotFound(404),
        MethodNotAllowed(405),
        NotAcceptable(406),
        ProxyAuthenticationRequired(407),
        RequestTimeout(408),
        Conflict(409),
        Gone(410),
        LengthRequired(411),
        ExpectationFailed(417),
        ImATeapot(418),

        InternalServerError(500),
        NotImplemented(501),
        BadGateway(502),
        ServiceUnavailable(503),
        GatewayTimeout(504),
        HTTPVersionNotSupported(505),
        ;

        private final int code;

        IANACode(int code) {
            this.code = code;
        }

        @Override
        public int code() {
            return code;
        }
    }
}
