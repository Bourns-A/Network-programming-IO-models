package server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

public class ChatServer {

    private static final int  DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    private ServerSocketChannel server;
    private Selector selector;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER); //转发消息
    private Charset charset = Charset.forName("UTF-8");
    private int port;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
    }

    private void start() {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false); //设置非阻塞式调用
            server.socket().bind(new InetSocketAddress(port)); //绑定端口

            selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT); //注册channel到selector
            System.out.println("Server Ready, Monitoring port:"+port+"...");

            while (true) {
                selector.select(); //select的调用是阻塞式的
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key: selectionKeys) {
                    //处理被触发的事件
                    handles(key);
                }
                selectionKeys.clear();
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(selector);
        }
    }

    private void handles(SelectionKey key) throws IOException {
        //ACCEPT 事件触发：和客户端建立连接
        if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false); //转为非阻塞式调用
            client.register(selector, SelectionKey.OP_READ);
            System.out.println(getClientName(client) +"connected");
        }

        //READ 事件：客户端给服务器端发送消息
        else if (key.isReadable()) {
            SocketChannel client = (SocketChannel) key.channel();
            String fwdMsg = receive(client);
            if (fwdMsg.isEmpty()) {
                //客户端异常
                key.cancel();
                selector.wakeup();
            } else {
                System.out.println(getClientName(client)+":"+fwdMsg);
                forwardMessage(client, fwdMsg);

                //检查是否退出
                if (readyToQuit(fwdMsg)) {
                    key.cancel();
                    selector.wakeup();
                    System.out.println(getClientName(client)+" Disconnected");
                }
            }
        }
    }

    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        while(client.read(rBuffer) >0) ;
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    private void forwardMessage(SocketChannel client, String fwdMsg) throws IOException {
        for (SelectionKey key: selector.keys()) {
            if (key.isValid() &&key.channel() instanceof SocketChannel) {
                SocketChannel connectedClient = (SocketChannel) key.channel();
                if (!client.equals(connectedClient)) {
                    wBuffer.clear();
                    wBuffer.put(charset.encode(getClientName(client)+":"+fwdMsg));
                    wBuffer.flip();
                    while (wBuffer.hasRemaining()) {
                        connectedClient.write(wBuffer);
                    }
                }
            }
        }
    }



    private String getClientName(SocketChannel client) {
        return "Client["+client.socket().getPort()+"] Connected";
    }

    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private void close(Closeable closeable) {
        if (closeable !=null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer(7777);
        chatServer.start();
    }
}
