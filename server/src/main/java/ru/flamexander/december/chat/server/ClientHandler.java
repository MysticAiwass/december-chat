package ru.flamexander.december.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Server server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String username;

    private static int clientsCount = 0;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        clientsCount++;
        this.username = "user" + clientsCount;

        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = in.readUTF();
                if (message.startsWith("/")) {
                    if (message.equals("/exit")) {
                        break;
                    }
                    if (message.startsWith("/w ")) {
                        processPrivateMessage(message);
                    }
                } else {
                    server.broadcastMessage(username + ": " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processPrivateMessage(String message) {
        try {
            String[] parts = message.split(" ", 3);
            if (parts.length == 3) {
                String recipient = parts[1];
                String privateMessage = parts[2];

                boolean recipientExists = server.isUsernameExists(recipient);

                if (recipientExists) {
                    server.sendPrivateMessage(this, recipient, privateMessage);
                } else {
                    out.writeUTF("Пользователь '" + recipient + "' не существует.");
                }
            } else {
                out.writeUTF("Неверный формат личного сообщения. Используйте: /w получатель сообщение");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}