package connector;

import java.io.IOException;
import java.io.InputStream;

public class Request {

    private static final int BUFFER_SIZE = 1024;
    private InputStream input;
    private String uri;

    public Request(InputStream input) {
        this.input = input;
    }

    public String getRequestURI() {
        return this.uri;
    }

    public void parse() {
        int length = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            length = input.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder request = new StringBuilder();
        for (int j=0; j<length; j++) {
            request.append((char)buffer[j]);
        }
        uri = parseURI(request.toString());
    }

    private String parseURI(String request) {
        int idx1, idx2;
        idx1 = request.indexOf(' ');
        if (idx1!=-1) {
            idx2 = request.indexOf(' ', idx1+1);
            if (idx2 >idx1) {
                return request.substring(idx1+1, idx2);
            }
        }
        return "";
    }

}
