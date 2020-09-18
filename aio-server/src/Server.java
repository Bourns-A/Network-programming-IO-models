import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;

public class Server {
    final static String LOCALHOST = "localhost";
    final int DEFAULT_PORT = 8888;
    AsynchronousServerSocketChannel serverChannel;

    public void start() {

        try {
            //绑定监听端口
            //server在系统底层绑定了默认的asynchronousChannelGroup对象。它类似于线程池，提供异步通道和共享的系统资源
            //回调函数的线程则在此对象中
            serverChannel = AsynchronousServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));
            System.out.println("Server online, monitoring" + DEFAULT_PORT);

            while (true) { //防止服务器过早返回
                serverChannel.accept(null, new AcceptHandler());
                System.in.read(); //进行阻塞，避免循环频繁调用
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverChannel);
        }
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> { //异步调用Accept函数

        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {  //回调函数
            if (serverChannel.isOpen()) {
                serverChannel.accept(null, this);  //stack调用accept层级在底层被限制，溢出会被限制
            }
            AsynchronousSocketChannel clientChannel = result;
            if (clientChannel !=null && clientChannel.isOpen()) {
                ClientHandler handler = new ClientHandler(clientChannel); //处理客户端读写操作完毕的handler
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                Map<String, Object> info = new HashMap<>();
                info.put("type", "read");
                info.put("buffer", buffer);
                clientChannel.read(buffer, info, handler);  //异步调用read
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
        // 处理错误情况

        }
    }

    private class ClientHandler implements CompletionHandler<Integer, Object> { //异步调用clientchannel的read和write操作，用clienthandler处理操作结果
        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            Map<String, Object> info = (Map<String, Object>) attachment;
            String type = (String) info.get("type");
            if ("read".equals(type)) {
                ByteBuffer buffer = (ByteBuffer) info.get("buffer");
                buffer.flip(); //读变写
                info.put("type", "write");
                clientChannel.write(buffer, info, this);
                buffer.clear();
            } else if ("write".equals(type)) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                //已经从回调函数的参数里得到了map
                info.put("type", "read");
                info.put("buffer", buffer);
                clientChannel.read(buffer, info, this);  //异步调用read
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {

        }
    }

    public static void main(String[] args) {

        Server server = new Server();
        server.start();
    }
}
