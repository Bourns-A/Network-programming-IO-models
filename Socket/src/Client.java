import java.io.*;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        final String QUIT = "quit";
        final String DEFAULT_SERVER_HOST = "127.0.0.1";
        final int DEFAULT_SERVER_PORT = 8888;
        Socket socket = null;
        BufferedWriter writer = null;
        try {
            //创建socket
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
            //创建IO流
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); //装饰器模式
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            //等待输入信息
            BufferedReader consoloReader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                String input = consoloReader.readLine();
                //信息发送给服务器
                writer.write(input + "\n");
                writer.flush();

                //读取服务器返回消息
                String msg = reader.readLine();
                System.out.println(msg);
                //检查用户是否退出
                if (QUIT.equals(input)){break;}
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer!=null){
                try {
                    writer.close();
                    System.out.println("writer close");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
