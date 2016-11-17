package Main;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class SerialPortComm extends Thread {

    public static void main(String[] args) {
        SerialPortComm spc = new SerialPortComm();

        spc.initialize("null");

    }

    Process p = null;

    int p1 = 0;
    int p2 = 0;

    @Override
    public void run() {

        InputStream is = p.getInputStream();

        Scanner s = new Scanner(is);

        while (s.hasNext("Waiting")) {
            System.out.println("Waiting");
        }

        //s.useDelimiter(";");

        Pattern p = Pattern.compile(".*:.*;");
        
        while (!this.isInterrupted()) {

            if (s.hasNext(p)) {
                
                String s1 = s.next(p);
                
                if (s1.isEmpty()) {
                    continue;
                }
                
                String[] string = s1.split(":");

                try {
                    p1 = Integer.valueOf(string[0]);
                    p2 = Integer.valueOf(string[1]);
                } catch (Exception e) {

                }

            }else if(s.hasNext()){
                s.next();
            }

            Thread.yield();
        }

    }

    public void initialize(String portName) {
        try {
            p = Runtime.getRuntime().exec("python /path/to/serialIn.py " + portName);
        } catch (IOException ex) {
            Logger.getLogger(SerialPortComm.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.start();
    }

    public void closeConnection() {
        if (p.isAlive()) {
            p.destroy();
        } else {
            System.err.println("Tried closing Serial Port Reader, but was closed already");
        }
    }

}
