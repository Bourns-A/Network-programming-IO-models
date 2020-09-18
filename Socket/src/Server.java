import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        final String QUIT = "quit";
        final int DEFAULT_PORT = 8888;
        ServerSocket serverSocket = null;

        try {
            //绑定监听端口
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("监听端口"+DEFAULT_PORT);

            while (true) {
                //等待客户端链接
                Socket socket = serverSocket.accept(); //accept阻塞式调用
                System.out.println("client["+socket.getPort()+"] connected");
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                String msg = null;
                while ((msg = reader.readLine()) !=null) {
                    //读取客户端消息
                    System.out.println("client["+socket.getPort()+"]" + msg);
                    //服务器把客户消息回复
                    writter.write("server: " + msg +"\n");
                    writter.flush(); //防止消息留在缓冲区
                    if (QUIT.equals(msg)) {
                        System.out.println("Client["+socket.getPort()+"] Disconnected");
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket !=null) {
                try {
                    serverSocket.close();
                    System.out.println("Server close");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
