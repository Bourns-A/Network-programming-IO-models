package client;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

public class ChatClient {
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    private String host;
    private int port;
    private SocketChannel client;
    private ByteBuffer rBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer wBuffer = ByteBuffer.allocate(1024);
    private Selector selector;
    private Charset charset = Charset.forName("UTF-8");

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public  ChatClient() {
        this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private void start() {
        try {
            client = SocketChannel.open();
            client.configureBlocking(false);

            selector = Selector.open();
            client.register(selector, SelectionKey.OP_CONNECT);
            client.connect( new InetSocketAddress(host, port));

            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key: selectionKeys) {
                    handles(key);

                }
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClosedSelectorException e) {
            //用户正常退出
        } finally {
            close(selector); //在已经关闭的selector上调用close，不会有异常抛出（不会真正执行）
        }
    }

    private void handles(SelectionKey key) throws IOException {
        //连接就绪事件
        if (key.isConnectable()) {
            SocketChannel client = (SocketChannel) key.channel();
            if (client.isConnectionPending()) { //true:连接就绪，建立连接的操作在进行中。false：需要继续等待
                client.finishConnect();
                //处理用户输入
                new Thread(new UserInputHandler(this)).start(); //一个线程专门等待用户在控制台的输入
            }
            client.register(selector, SelectionKey.OP_READ);
        //READ事件 - 服务器转发消息
        } else if (key.isReadable()) {
            SocketChannel client = (SocketChannel) key.channel();
            String msg = receive(client);
            if (msg.isEmpty()) {
                //服务器出现异常
                close(selector);
            } else {
                System.out.println(msg);
            }
        }

    }

    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        while (client.read(rBuffer) >0);
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    public void send(String msg) throws IOException {
        if (msg.isEmpty()) {
            return ;
        }
        wBuffer.clear();
        wBuffer.put(charset.encode(msg));
        wBuffer.flip();
        while (wBuffer.hasRemaining()) {
            client.write(wBuffer);
        }
        if (readyToQuit(msg)) {
            close(selector);
        }
    }

    public void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient("127.0.0.1", 7777);
        client.start();

    }
}
