package connector;

import org.junit.Assert;
import org.junit.Test;
import util.TestUtils;

public class RequestTest {

    private static final String validRequest = "GET /index.html HTTP/1.1";

    @Test
    public void givenValidRequest_thenExtrackUri() {
        Request request = TestUtils.createRequest(validRequest);
        Assert.assertEquals("/index.html", request.getRequestURI());
    }
}
