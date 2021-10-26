import Function.FFunction;
import Function.GFunction;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;

// Допоміжний класс (для графічного вікна)
class MyActionListener extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            Main.reset();
            Main.begin();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class Main {

    // Графічне вікно
    private static final JFrame mainWindow = new JFrame("Operation system");
    private static final JLabel xField = new JLabel();
    private static final JLabel resultField = new JLabel();
    private static final JLabel logField = new JLabel("start in 5 second.");

    // Процеси з функціями
    private static Thread FFunctionServ = new Thread(new FFunction());
    private static Thread GFunctionServ = new Thread(new GFunction());
    private static AFUNIXSocket FfuncSocket;
    private static AFUNIXSocket GfuncSocket;

    // Зчитування/запис даних у процеси
    private static int resultF = 0;
    private static int resultG = 0;
    private static Integer result = 0;
    private static BufferedReader reader;
    private static BufferedReader inputFfunc;
    private static BufferedWriter outputFfunc;
    private static BufferedReader inputGfunc;
    private static BufferedWriter outputGfunc;

    public static void main(String[] args) throws InterruptedException, IOException {
        setUp();
    }

    // Встановлення графічного вікна
    private static void setUp() throws InterruptedException, IOException {
        mainWindow.setSize(300, 120);

        Container pane = mainWindow.getContentPane();
        JLabel inputX = new JLabel("x value: ");
        JLabel outputResult = new JLabel("Result: ");
        JLabel logs = new JLabel("Log: ");
        pane.setLayout(new GridLayout(0, 2, 3, 5));
        inputX.setHorizontalAlignment(JLabel.RIGHT);
        pane.add(inputX);
        pane.add(xField);
        outputResult.setHorizontalAlignment(JLabel.RIGHT);
        pane.add(outputResult);
        pane.add(resultField);
        logs.setHorizontalAlignment(JLabel.RIGHT);
        pane.add(logs);
        pane.add(logField);

        // Встановлення reset на ctrl+z
        KeyStroke keStroke = KeyStroke.getKeyStroke("ctrl Z");
        Action action = new MyActionListener();
        InputMap iMap = inputX.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = inputX.getActionMap();
        iMap.put(keStroke, "Action");
        actionMap.put("Action", action);

        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainWindow.setVisible(true);
        Thread.sleep(5000);
        logField.setText("processing");
        logField.paintImmediately(logField.getVisibleRect());
        begin();
    }

    // Старт роботи з process F and G
    public static void begin() throws InterruptedException, IOException {
        FFunctionServ.start();
        GFunctionServ.start();

        Thread.sleep(500);
        try {
            FfuncSocket = AFUNIXSocket.newInstance();
            GfuncSocket = AFUNIXSocket.newInstance();

            FfuncSocket.connect(AFUNIXSocketAddress.of(new File("serF.txt"), 8000));
            FfuncSocket.setSoTimeout(10000);
            GfuncSocket.connect(AFUNIXSocketAddress.of(new File("serG.txt"), 8001));
            GfuncSocket.setSoTimeout(10000);

            reader = new BufferedReader(new InputStreamReader(new FileInputStream("input.txt")));
            inputFfunc = new BufferedReader(new InputStreamReader(FfuncSocket.getInputStream()));
            outputFfunc = new BufferedWriter(new OutputStreamWriter(FfuncSocket.getOutputStream()));

            inputGfunc = new BufferedReader(new InputStreamReader(GfuncSocket.getInputStream()));
            outputGfunc = new BufferedWriter(new OutputStreamWriter(GfuncSocket.getOutputStream()));

            String x = reader.readLine();
            xField.setText(x);
            xField.paintImmediately(xField.getVisibleRect());

            outputFfunc.write(x + "\n");
            outputGfunc.write(x + "\n");

            outputFfunc.flush();
            outputGfunc.flush();

            // Log
            System.out.println("x was sent");
            logField.setText("x was sent");
            logField.paintImmediately(logField.getVisibleRect());

            String resultStringF, resultStringG;
            try {
                boolean firstServerDone = inputFfunc.ready();
                boolean secondServerDone = inputGfunc.ready();
                while (!firstServerDone || !secondServerDone) {
                    if (inputFfunc.ready()) {
                        resultStringF = inputFfunc.readLine();
                        System.out.println("f is ready");
                        System.out.println("f(x) = " + resultStringF);
                        logField.setText("f is ready");
                        logField.paintImmediately(logField.getVisibleRect());
                        try {
                            resultF = Integer.valueOf(resultStringF);
                        } catch (NumberFormatException e) {
                            logField.setText("fail (hard)");
                            logField.paintImmediately(logField.getVisibleRect());
                            throw new IOException();
                        }
                        firstServerDone = true;
                    }
                    if (inputGfunc.ready()) {
                        resultStringG = inputGfunc.readLine();
                        System.out.println("g is ready");
                        System.out.println("g(x) = " + resultStringG);
                        logField.setText("g is ready");
                        try {
                            resultG = Integer.valueOf(resultStringG);
                        } catch (NumberFormatException e) {
                            logField.setText("fail (fail)");
                            logField.paintImmediately(logField.getVisibleRect());
                            throw new IOException();
                        }
                        secondServerDone = true;
                    }
                }
                result = resultF & resultG;
                System.out.println("serverF & serverG: "  + result);
                resultField.setText(result.toString());
                resultField.paintImmediately(resultField.getVisibleRect());

                logField.setText("finish");
                logField.paintImmediately(logField.getVisibleRect());
            } catch (IOException e) {
                FFunctionServ.interrupt();
                GFunctionServ.interrupt();
            }

        } finally {
            FfuncSocket.close();
            GfuncSocket.close();
            reader.close();
            inputFfunc.close();
            outputFfunc.close();
            inputGfunc.close();
            outputGfunc.close();
            System.out.println("Close all process");
        }
    }

    // Метод для hard reset
    public static void reset() throws IOException, InterruptedException {
        System.out.println("reset");
        xField.setText("");
        xField.paintImmediately(xField.getVisibleRect());
        resultField.setText("");
        resultField.paintImmediately(resultField.getVisibleRect());
        logField.setText("reset");
        logField.paintImmediately(logField.getVisibleRect());

        reader.close();
        inputFfunc.close();
        inputGfunc.close();
        outputFfunc.close();
        outputGfunc.close();

        FFunctionServ.interrupt();
        GFunctionServ.interrupt();
        FFunctionServ = new Thread(new FFunction());
        GFunctionServ = new Thread(new GFunction());
    }
}