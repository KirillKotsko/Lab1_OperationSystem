package Function;

import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.*;

public class GFunction implements Runnable{
    private AFUNIXSocket client;
    private AFUNIXServerSocket process;

    private int x;

    private int g() throws InterruptedException {
        // Емуляція довгих підрахунків
        Thread.sleep(8000);

        return x + 2;
    }

    @Override
    public void run(){
        BufferedReader input;
        BufferedWriter output;
        try {
            process = AFUNIXServerSocket.newInstance();
            AFUNIXSocketAddress address = AFUNIXSocketAddress.of(new File("serG.txt"), 8001);
            process.bind(address);
            client = process.accept();

            input = new BufferedReader(new InputStreamReader(client.getInputStream()));
            output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

            String inputX = input.readLine();
            x = Integer.parseInt(inputX);

            output.write(g()  + "\n");

            output.flush();
            input.close();
            output.close();
        } catch (IOException | InterruptedException ignore) {
        } finally {
            try {
                client.close();
                System.out.println("Close g process");
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
    }
}