package util;

import connector.Request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestUtils {

    public static Request createRequest(String requestStr) {
        InputStream input = new ByteArrayInputStream(requestStr.getBytes());
        Request request = new Request(input);
        request.parse();
        return request;
    }

    public static String readFileToString(String filename) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filename)));
    }
}