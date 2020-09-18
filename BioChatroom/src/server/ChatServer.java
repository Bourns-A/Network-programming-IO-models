package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";

    private ExecutorService executorService;
    private ServerSocket serverSocket;
    private Map<Integer, Writer> connectedClients;

    public ChatServer() {
        //已经存在的用户
        executorService = Executors.newFixedThreadPool(10);
        connectedClients = new HashMap<>();
    }

    //添加上线用户
    //异常抛出，不在函数内部处理
    public synchronized void addClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            connectedClients.put(port, writer);
            System.out.println("Client["+port+"] connected");

        }
    }

    //移除用户
    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            if (connectedClients.containsKey(port)) {
                connectedClients.get(port).close();
            }
            connectedClients.remove(port);
            System.out.println("client["+port+"] disconnected");
        }
    }

    public synchronized void forwardMessage(Socket socket, String fwdMsg) throws IOException {
        for (Integer id: connectedClients.keySet()) {
            if (!id.equals(socket.getPort())) {
                Writer writer = connectedClients.get(id);
                writer.write(fwdMsg);
                writer.flush();
            }
        }
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public synchronized void close() {
        if(serverSocket !=null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        try {
            //绑定监听端口
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("Server online...");

            while (true) {
                //等待客户端连接. Accept是阻塞式调用
                Socket socket = serverSocket.accept();
                //创建chatHandler线程
                //new Thread(new ChatHandler(this, socket)).start();
                //使用线程池
                executorService.execute(new ChatHandler(this, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }

}
