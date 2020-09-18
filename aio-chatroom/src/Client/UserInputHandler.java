package Client;

import java.io.*;

import java.io.InputStreamReader;

public class UserInputHandler implements Runnable {

    private ChatClient chatClient;

    public UserInputHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }


    @Override
    public void run() {
        try{
            //等待用户输入消息
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String input = consoleReader.readLine();
                chatClient.send(input);
                //检查用户退出
                if (chatClient.readyToQuit(input)) break;

            }

        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
