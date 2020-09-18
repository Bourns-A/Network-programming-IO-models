package Client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ChatClient {
    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    private String host;
    private int port;
    private AsynchronousSocketChannel clientChannel;
    private Charset charset = Charset.forName("UTF-8");

    public ChatClient() {
        this(LOCALHOST, DEFAULT_PORT);
    }

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private void start() {

        try {
            //创建socketChannel
            clientChannel = AsynchronousSocketChannel.open();
            Future<Void> future = clientChannel.connect(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));
            future.get();
            //启动新的线程实现用户输入
            new Thread(new UserInputHandler(this)).start();
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER);
            while (true) {
                Future<Integer> readResult = clientChannel.read(buffer);
                int result = readResult.get();
                if (result <=0) {
                    System.out.println("Server Disconnected");
                    close(clientChannel);
                    System.exit(1);
                } else {
                    buffer.flip();
                    //解码buffer重的数据
                    String msg = String.valueOf(charset.decode(buffer));
                    buffer.clear();
                    System.out.println(msg);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void send(String msg) {
        if (msg.isEmpty()) {
            return;
        } else {
            ByteBuffer buffer = charset.encode(msg);
            Future<Integer> writeResult = clientChannel.write(buffer);
            try {
                writeResult.get();
            } catch (InterruptedException| ExecutionException e) {
                System.out.println("Message Send Failed");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.start();
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }


}
