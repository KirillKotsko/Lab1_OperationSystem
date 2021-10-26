package Function;

import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.*;

public class FFunction implements Runnable {
    private AFUNIXSocket client;
    private AFUNIXServerSocket process;

    private int x;

    private int f() throws InterruptedException {
        // Емуляція довгих підрахунків
        Thread.sleep(5000);

        return x+1;
    }

    @Override
    public void run(){
        try {
            BufferedReader input;
            BufferedWriter output;

            process = AFUNIXServerSocket.newInstance();
            AFUNIXSocketAddress address = AFUNIXSocketAddress.of(new File("serF.txt"), 8000);
            process.bind(address);
            client = process.accept();

            input = new BufferedReader(new InputStreamReader(client.getInputStream()));
            output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

            String inputX = input.readLine();
            x = Integer.parseInt(inputX);

            output.write(f() + "\n");

            output.flush();
            input.close();
            output.close();
        } catch (IOException | InterruptedException ignore) {
        } finally {
            try {
                client.close();
                System.out.println("Close f process");
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
    }
}