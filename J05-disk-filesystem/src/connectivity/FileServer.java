package connectivity;

import com.google.gson.Gson;
import disk.Controller;
import disk.DiskController;
import filesystem.FileSystem;
import filesystem.SimpleFile;
import server_response_objects.*;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.List;

public class FileServer {
    private static final Charset m_charset = Charset.defaultCharset();
    private static final String DIR = "..";
    private final static int NUM_BLOCKS = 50;
    private final static int BLOCK_SIZE = 4096;
    private final static Controller controller = DiskController.getInstance(DIR, NUM_BLOCKS, BLOCK_SIZE);
    private static FileSystem fileSystem;
    private final static Gson gson = new Gson();
    public static void run() throws IOException {
        new Thread(() -> {
            try (var serverSocket = ServerSocketChannel.open()) {
                serverSocket.bind(new InetSocketAddress(4242));
                for (;;) {
                    var client = serverSocket.accept();
                    var t = new Thread(() -> handleClient(client));
                    t.start();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        for (;;) {
            try (var socket = new DatagramSocket(4242)) {
                byte[] buf = new byte[256];
                var packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                var response = "Hello World".getBytes();
                packet = new DatagramPacket(response, response.length, address, port);
                socket.send(packet);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private static void handleClient(SocketChannel client) {
        try (client) {
            startTalkingToClient(client);
        }
        catch (IOException e) {
            System.out.println("Error from client");
        }
    }
    private static void startTalkingToClient(SocketChannel client) throws IOException {
        System.out.println("Got connection from:  " + client.getRemoteAddress());

        var buffer = ByteBuffer.allocate(1024);
        while (client.read(buffer) > 0) {
            buffer.flip();
            var userInput = m_charset.decode(buffer).toString();
            System.out.printf("%s\n", userInput);
            String output = handleClientRequest(userInput);
            var response = m_charset.encode(output);
            client.write(response);
            buffer.clear();
        }
    }

    private static String handleClientRequest(final String userInput) {
        String[] inputParts = userInput.split(" ");
        return switch (inputParts[0].toUpperCase()) {
            case "USE" -> openDisk(inputParts[1]);
            case "DIR" -> GetFileNames();
            case "CREATE" -> CreateNewFile(inputParts[1]);
            case "DATA" -> OpenFile(inputParts[1]);
            case "INFO" -> GetFileInfo(inputParts[1]);
            case "REMOVE" -> fileSystem.Delete(inputParts[1]) ? gson.toJson(new SingleResultResponse("OK")) : gson.toJson(new SingleResultResponse("Fail"));
            default -> "Unsupported command - " + inputParts[0];
        };
    }

    private static String GetFileInfo(final String fileName) {
        final int fileSize = fileSystem.getfileSize(fileName);
        var response = new InfoResponse(fileName, fileSize);
        if (fileSize < 0) {
            var failedResponse = new SingleResultResponse("No such file in system");
            return gson.toJson(failedResponse);
        }
        return gson.toJson(response);
    }

    private static String OpenFile(final String fileName) {
        final SimpleFile file = fileSystem.Open(fileName);
        final byte[] content = file.read();
        var response = new DataResponse(fileName, content);
        return gson.toJson(response);
    }

    private static String CreateNewFile(final String fileName) {
        SimpleFile newFile = fileSystem.newFile(fileName);
        SingleResultResponse response = new SingleResultResponse("OK");
        if (newFile == null) {
            response = new SingleResultResponse("Fail");
        }
        return gson.toJson(response);
    }

    private static String GetFileNames() {
        final List<String> files = fileSystem.dir();
        var response = new DirResponse("OK", files);
        return gson.toJson(response);
    }

    private static String openDisk(final String diskNumString) {
        final int diskNum = Integer.parseInt(diskNumString);
        var disk = controller.get(diskNum);
        fileSystem = new FileSystem(disk);
        var response = new SingleResultResponse("OK");
        return gson.toJson(response);
    }

}