package Server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final String  LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;
    private static final int THREADPOOL_SIZE = 8;
    private List<ClientHandler> connectedClient;

    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverChannel;
    private Charset charset = Charset.forName("UTF-8");
    private int port;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
        this.connectedClient = new ArrayList<>();


    }

    public void start() {
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(THREADPOOL_SIZE);
            channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
            serverChannel = AsynchronousServerSocketChannel.open(channelGroup);
            serverChannel.bind(new InetSocketAddress(LOCALHOST, port));
            System.out.println("Server online, monitoring" + port);

            while (true) {
                serverChannel.accept(null, new AcceptHandler());
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverChannel);

        }
    }

    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer(8888);
        server.start();
    }

    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
            if (serverChannel.isOpen()) {
                serverChannel.accept(null, this);
            }
            if (clientChannel !=null && clientChannel.isOpen()) {
                ClientHandler handler = new ClientHandler(clientChannel);  //为每个客户都创建一个新的ClientHandler
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER);
                // add client into client list
                addClient(handler);
                clientChannel.read(buffer, buffer, handler);  //buffer 作为写入对象， buffer作为 attachment
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("Connection failed"+exc);
        }
    }

    //把client加到list中
    private synchronized void addClient(ClientHandler handler) {
        connectedClient.add(handler);
        System.out.println(getClientName(handler.clientChannel) + "Connected");
    }

    //把client移除
    private synchronized void removeClient(ClientHandler handler) {
        connectedClient.remove(handler);
        System.out.println(getClientName(handler.clientChannel) +"Disconnected");
        close(handler.clientChannel);

    }

    private class ClientHandler implements CompletionHandler<Integer, Object> {
        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            ByteBuffer buffer = (ByteBuffer) attachment;
            if (buffer !=null) {
                if (result <=0) {  //客户端通道异常
                    // 从客户list中移除该客户
                    removeClient(this);
                } else {
                    //读取buffer数据
                    buffer.flip();
                    String fwdMsg = receive(buffer);
                    System.out.println(getClientName(clientChannel) +":" +fwdMsg);
                    forwardMessage(clientChannel, fwdMsg);
                    buffer.clear();
                    //quit
                    if(readyToQuit(fwdMsg)) {
                        removeClient(this);
                    } else {
                        clientChannel.read(buffer, buffer, this);
                    }
                }
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("Read or Write Failed"+exc);
        }
    }

    private synchronized void forwardMessage(AsynchronousSocketChannel clientChannel, String fwdMsg) {
        for (ClientHandler handler: connectedClient) {
            if (!clientChannel.equals(handler.clientChannel)) {
                try {
                    ByteBuffer buffer = charset.encode(getClientName(handler.clientChannel)+":" +fwdMsg);
                    handler.clientChannel.write(buffer, null, handler);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getClientName(AsynchronousSocketChannel clientChannel) {
        int clientPort = -1;
        try {
            InetSocketAddress address = (InetSocketAddress) clientChannel.getRemoteAddress();
            clientPort = address.getPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Client["+clientPort+"]";
    }

    private String receive(ByteBuffer buffer) {
        CharBuffer charBuffer = charset.decode(buffer);
        return String.valueOf(charBuffer);
    }
}
