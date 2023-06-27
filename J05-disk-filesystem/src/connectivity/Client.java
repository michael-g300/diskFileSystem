package connectivity;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Scanner;

public class Client {
    private static final Charset m_charset = Charset.defaultCharset();
    public static void run() throws IOException {
        try(SocketChannel socket = SocketChannel.open()) {
            var address = new InetSocketAddress("localhost", 4242);
            socket.connect(address);

            try(var sc = new Scanner(System.in)){
                String line;
                do{
                    line = sc.nextLine();
                    var buffer = m_charset.encode(line);
                    socket.write(buffer);
                    buffer.clear();
                    buffer.flip();
                    var responseBuffer = ByteBuffer.allocate(1024);
                    socket.read(responseBuffer);
                    responseBuffer.flip();
                    var response = m_charset.decode(responseBuffer);
                    System.out.println(response);

                } while(!line.equalsIgnoreCase("q"));
                System.out.println("Done!");
            }
        }
        try(var socket = new DatagramSocket()) {
            var address = InetAddress.getByName("localhost");
            try(var sc = new Scanner(System.in)){

            }
            var buf = "Knock Knock!".getBytes();
            var packet = new DatagramPacket(buf, buf.length, address, 4242);
            socket.send(packet);

            buf = new byte[256];
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            var received = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Quote of the Moment: " + received);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}