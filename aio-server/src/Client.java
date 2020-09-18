import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Client {

    final String LOCALHOST = "localhost";
    final int DEFAULT_PORT = 8888;
    AsynchronousSocketChannel clientChannel;

    public void start() {
        try {
            //创建channel
            clientChannel = AsynchronousSocketChannel.open();
            Future<Void> future = clientChannel.connect(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));  //异步调用connect
            //使用future对象
            future.get();  //get是阻塞式操作

            BufferedReader consoloReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String input = consoloReader.readLine();
                //输入的信息写入通道
                byte [] inputBytes = input.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(inputBytes);
                Future<Integer> writeResult = clientChannel.write(buffer);
                writeResult.get();  //阻塞式调用，等待服务器返回数据
                buffer.flip();
                Future<Integer> readResult = clientChannel.read(buffer);
                readResult.get(); //阻塞式调用，等待read操作完成
                String echo = new String(buffer.array());
                buffer.clear();
                System.out.println(echo);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            close(clientChannel);
        }
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}
