package Main;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

//Made by Memcallen.
public class Main extends Thread {

    public static void main(String[] args) {
        Main main = new Main();
        main.start();
    }

    JFrame frame = new JFrame("Pong");
    JPanel panel = new JPanel();
    public static final Font SCOREFONT = new Font("Arial", 0, 24);
    public static volatile boolean LoadingScreen = true;
    public static volatile boolean doArduino = false;
    public static volatile String ArdP = "/dev/ttyUSB0";
    public static volatile SerialPortComm comm = new SerialPortComm();
    public static boolean p1AI = false;
    public static boolean p2AI = false;
    public static int p1AIDif = 0;
    public static int p2AIDif = 0;
    public static int bestofNum = 1;
    public static int winNum = 10;
    public static AI P1AI_ref = null;
    public static AI P2AI_ref = null;

    public enum Direction {
        UP, DOWN, NONE;
    }

    public enum Side {
        LEFT, RIGHT;
    }

    public enum Alignment {
        HORIZONTAL, VERTICAL;
    }

    public static class Vector {

        public int angle = 0;
        public double mag = 0;

        public Vector() {
            this(0, 0);
        }

        public static Vector random(double magL, double magH, int angL, int angH) {
            int mag1 = (int) ((magH - magL) * Math.random() + magL);
            int ang1 = (int) ((angH - angL) * Math.random() + angL);

            return new Vector(mag1, ang1);
        }

        public Point add(Point p) {
            int dx = (int) (Math.cos(Math.toRadians(angle)) * (mag + 1));
            int dy = (int) (Math.sin(Math.toRadians(angle)) * (mag + 1));

            p.x += dx;
            p.y += dy;

            return p;
        }

        public Vector(double magnitude, int angle) {
            this.mag = magnitude;
            this.angle = angle % 360;
        }

        @Override
        public String toString() {
            return mag + "[" + angle + "]";
        }
    }

    public class Player {

        public double location = 0;
        public int score = 0;
        public int wins = 0;
        public Side side;
        public Direction dir = Direction.NONE;
        private int x = 0;

        public static final double WIDTH = 2; // percent
        public static final double HEIGHT = 15; // percent
        public static final double OFFSET = 1;

        private Canvas canvas = null;

        public Player(Side s, Canvas c) {
            this.side = s;
            canvas = c;
        }

        public void setDirection(Direction d) {
            dir = d;
        }

        public int percentWidth(double perc) {
            return (int) (perc / 100 * canvas.getWidth());
        }

        public int percentHeight(double perc) {
            return (int) (perc / 100 * canvas.getHeight());
        }

        public Rectangle getRectangle() {
            return new Rectangle(x, percentHeight(location), percentWidth(WIDTH), percentHeight(HEIGHT));
        }

        public boolean isColliding(Ball ball) {
            Rectangle t = getRectangle();
            Rectangle b = ball.getRectangle();
            return t.intersects(b);
        }

        public void tick() {
            if (!doArduino) {
                switch (dir) {
                    case DOWN:
                        location = Math.max(Math.min(location - 2.5, 100 - HEIGHT), 1);
                        break;
                    case NONE:
                        break;
                    case UP:
                        location = Math.max(Math.min(location + 2.5, 100 - HEIGHT), 1);
                        break;
                }
            }

        }

        public void render(Graphics g, Canvas c) {
            switch (side) {
                case LEFT:
                    x = c.getWidth() - percentWidth(WIDTH + OFFSET);
                    break;
                case RIGHT:
                    x = percentWidth(OFFSET);
                    break;
            }

            g.setColor(Color.white);

            g.fillRect(x, percentHeight(location), percentWidth(WIDTH), percentHeight(HEIGHT));

        }

    }

    public final class Ball {

        public double x = 0;
        public double y = 0;

        public int diameter = 10;

        private Canvas c = null;

        Vector movement = null;

        public Ball(Canvas c) {
            this.c = c;
            this.reset();
        }

        private double lastX = 50;
        private double lastY = 50;
        private long last = System.currentTimeMillis();

        public double calculatePixelsPerSecond() {
            double pps;

            double d = Math.sqrt(Math.pow((x - lastX), 2) + Math.pow((y - lastY), 2));

            long deltaT = System.currentTimeMillis() - last;

            System.out.println((double) deltaT / 1000);

            pps = d / ((double) deltaT / 1000);

            lastX = x;
            lastY = y;
            last = System.currentTimeMillis();

            return pps;
        }

        private boolean within(int a, int b, int error) {
            return a > b - error && a < b + error;
        }

        public void reset() {
            x = 50 + (Math.random() > 0.5 ? -Math.random() * 10 : Math.random() * 10);
            y = 50;
            int angle = (int) (Math.random() * 359);
            // double mag = Math.abs(Math.sin(Math.toRadians(angle))) * 3;
            int error = 10;
            while (within(angle, 270, error) && within(angle, 90, error) && within(angle, 180, error)
                    && within(angle, 0, error)) {
                angle = (int) (Math.random() * 359);
                // mag = Math.abs(Math.sin(Math.toRadians(angle))) * 3;
            }
            movement = new Vector(2, angle);
        }

        public boolean outOfBounds() {
            return !c.getBounds().contains(getRectangle());
        }

        public void reflect(Alignment al) {
            switch (al) {
                case VERTICAL:
                    movement.angle = 180 - movement.angle;
                    break;
                case HORIZONTAL:
                    movement.angle = 0 - movement.angle;
                    break;
            }
        }

        public Rectangle getRectangle() {
            return new Rectangle((int) (c.getWidth() * (x / 100)), (int) (c.getHeight() * (y / 100)), diameter,
                    diameter);
        }

        public void render(Graphics g) {
            g.setColor(Color.white);

            g.fillOval((int) (c.getWidth() * (x / 100)), (int) (c.getHeight() * (y / 100)), diameter, diameter);
        }

        public void tick() {
            Point p = movement.add(new Point((int) x, (int) y));
            this.x = p.x;
            this.y = p.y;
        }

    }

    public class AI {

        private Player player = null;
        private Ball ball = null;
        private int dif = 1;

        public AI(Player player, Ball ball, int dif) {
            this.player = player;
            this.ball = ball;
            this.dif = dif;
        }

        private boolean within(double x, double location, int error) {
            return x > location - error && x < location + error;
        }

        private int temp = 0;

        public void tick() { // 1: random-ness, and 20 margins, 2:no random, 20
            // margins, 3: no random, 30 margins
            if (dif < 3
                    && (ball.x < 100 - 20 && player.side == Side.LEFT || ball.x > 20 && player.side == Side.RIGHT)) {
                if (dif == 1 && temp++ % 10 == 0) {
                    player.setDirection(Math.random() > 0.5 ? Direction.DOWN : Direction.UP);
                } else if (dif == 1 && temp % 10 == 5) {
                    player.setDirection(Math.random() > 0.5 ? Direction.NONE : player.dir);
                }
            } else {
                double location = player.location + Player.HEIGHT / 2;
                if (within(ball.y, location, 5)) {
                    player.setDirection(Direction.NONE);
                } else if (ball.y > location) {
                    player.setDirection(Direction.UP);
                } else {
                    player.setDirection(Direction.DOWN);
                }
            }

        }
    }

    public void showSetup() {

        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                System.out.println("Found laf " + info.getName());
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
        }

        final JFrame setup = new JFrame("Settings");

        setup.getContentPane().setLayout(new GridLayout(6, 2));

        JRadioButton p1AIb = new JRadioButton("Player 1 AI");
        JRadioButton p2AIb = new JRadioButton("Player 2 AI");
        final JTextField p1S = new JTextField("1");
        final JTextField p2S = new JTextField("1");
        JRadioButton UseArd = new JRadioButton("Use Arduino");
        final JTextField ardPort = new JTextField("COM3");
        final JTextField bestof = new JTextField("1");
        final JTextField winnum = new JTextField("10");
        JButton start = new JButton("Start");

        bestof.setToolTipText("Best out of:");
        winnum.setToolTipText("Number of points to win");

        JPanel c = (JPanel) setup.getContentPane();

        c.add(p1AIb);
        c.add(p2AIb);
        c.add(p1S);
        c.add(p2S);
        c.add(UseArd);
        c.add(ardPort);
        c.add(new JLabel("Scoring:"));
        c.add(new JPanel());
        c.add(bestof);
        c.add(winnum);
        c.add(new JPanel());
        c.add(start);

        ardPort.setEnabled(false);
        p1S.setEnabled(false);
        p2S.setEnabled(false);

        p1S.setToolTipText("The Difficulty for Player 1's AI (1-3)");
        p2S.setToolTipText("The Difficulty for Player 2's AI (1-3)");

        p1AIb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                p1AI = ((JRadioButton) e.getSource()).isSelected();
                p1S.setEnabled(p1AI);
            }
        });

        p2AIb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                p2AI = ((JRadioButton) e.getSource()).isSelected();
                p2S.setEnabled(p2AI);
            }
        });

        UseArd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ardPort.setEnabled(((JRadioButton) e.getSource()).isSelected());
                ArdP = ardPort.getText();
                doArduino = ((JRadioButton) e.getSource()).isSelected();
            }
        });

        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArdP = ardPort.getText();
                boolean run = doArduino ? (new File(ArdP)).exists() || ArdP.equals("null") : true;
                boolean temp = true;
                try {
                    bestofNum = Integer.valueOf(bestof.getText());
                    temp = false;
                    winNum = Integer.valueOf(winnum.getText());
                } catch (Exception e_) {
                    JOptionPane.showMessageDialog(null, "Please enter a valid "
                            + (temp ? "Best of Number" : "Win Amount") + ".  Must be any whole number (1, 2, 3...)");
                    run = false;
                }
                try {
                    int p1D = Integer.valueOf(p1S.getText());
                    int p2D = Integer.valueOf(p2S.getText());
                    if (p1D > 3 || p1D < 1 || p2D > 3 || p2D < 1) {
                        JOptionPane.showMessageDialog(null, (p1D > 3 || p1D < 1 ? "Player 1" : "Player 2")
                                + "'s Difficulty is out of bounds. Must be 1, 2 or 3");
                        run = false;
                    }
                } catch (NumberFormatException | HeadlessException e_) {
                    JOptionPane.showMessageDialog(null, "Please enter a valid Difficulty.  Must be 1, 2 or 3");
                    run = false;
                }
                if (run) {
                    p1AIDif = Integer.valueOf(p1S.getText());
                    p2AIDif = Integer.valueOf(p2S.getText());
                    LoadingScreen = false;
                    setup.setVisible(false);
                } else if (doArduino && !(new File(ArdP)).exists()) {
                    System.err.println("That Port isn't available, try and add read/write perms to it");
                }
            }
        });

        setup.setLocationByPlatform(true);
        setup.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setup.pack();
        setup.setVisible(true);
    }

    @Override
    public void run() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationByPlatform(true);

        final Canvas canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(500, 500 / 16 * 9));
        canvas.setIgnoreRepaint(true);
        frame.add(canvas);

        frame.setIgnoreRepaint(true);
        frame.setResizable(false);

        frame.setLocationByPlatform(true);
        frame.pack();
        frame.getContentPane().requestFocus();

        final Player one = new Player(Side.RIGHT, canvas);
        final Player two = new Player(Side.LEFT, canvas);
        final Ball ball = new Ball(canvas);

        JPanel t = (JPanel) (frame.getContentPane());

        ActionMap am = t.getActionMap();

        InputMap im = t.getInputMap();

        im.put(KeyStroke.getKeyStroke('w'), "P1_Up");
        im.put(KeyStroke.getKeyStroke('s'), "P1_Down");
        im.put(KeyStroke.getKeyStroke('o'), "P2_Up");
        im.put(KeyStroke.getKeyStroke('l'), "P2_Down");
        im.put(KeyStroke.getKeyStroke('t'), "reset");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true), "P1_R");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true), "P1_R");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, 0, true), "P2_R");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, true), "P2_R");

        am.put("P1_Up", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!p1AI) {
                    one.setDirection(Direction.DOWN);
                }
            }
        });
        am.put("P1_Down", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!p1AI) {
                    one.setDirection(Direction.UP);
                }
            }
        });
        am.put("P2_Up", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!p2AI) {
                    two.setDirection(Direction.DOWN);
                }
            }
        });
        am.put("P2_Down", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!p2AI) {
                    two.setDirection(Direction.UP);
                }
            }
        });
        am.put("reset", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                ball.reset();
            }
        });

        Action P1_R = new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!p1AI) {
                    one.setDirection(Direction.NONE);
                }
            }
        };

        Action P2_R = new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!p2AI) {
                    two.setDirection(Direction.NONE);
                }
            }
        };

        am.put("P1_R", P1_R);
        am.put("P2_R", P2_R);

        canvas.createBufferStrategy(2);

        BufferStrategy buffer = canvas.getBufferStrategy();

        Graphics graphics = null;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long pre;

                int tps = 20;
                long millisPs = 1000 / tps;

                while (!Main.interrupted()) {
                    pre = System.currentTimeMillis();
                    one.tick();
                    two.tick();
                    ball.tick();

                    if (p1AI) {
                        P1AI_ref.tick();
                    }
                    if (p2AI) {
                        P2AI_ref.tick();
                    }

                    //TODO this
                    if (doArduino) {
                        one.location = comm.p1 - one.percentHeight(Player.HEIGHT) / 2;
                        two.location = comm.p2 - one.percentHeight(Player.HEIGHT) / 2;
                    }

                    if (ball.outOfBounds()) {
                        if (ball.x > 50) {
                            one.score++;
                        } else if (ball.x < 50) {
                            two.score++;
                        } else {
                            System.out.println("WAT");
                        }
                        ball.reset();
                    }
                    if (one.isColliding(ball)) {
                        ball.reflect(Alignment.VERTICAL);
                    }
                    if (two.isColliding(ball)) {
                        ball.reflect(Alignment.VERTICAL);
                    }

                    Rectangle c = canvas.getBounds();
                    Rectangle b = ball.getRectangle();
                    c.x -= 20; // make it wider, and shorter
                    c.width += 40;
                    c.y += b.height + 8;
                    c.height -= (b.height + 8) * 2;

                    if (!c.intersects(b)) {
                        ball.reflect(Alignment.HORIZONTAL);
                    }

                    if (one.score == winNum) {
                        one.wins++;
                        if ((double) one.wins / (double) bestofNum > 0.5) {
                            JOptionPane.showMessageDialog(null, "Player one wins " + one.score + "/" + bestofNum + "!");
                            System.exit(0);
                        } else {
                            one.score = 0;
                        }
                        ball.reset();
                        JOptionPane.showMessageDialog(null, "Player one wins!");
                    }
                    if (two.score == winNum) {
                        two.wins++;
                        if ((double) two.wins / (double) bestofNum > 0.5) {
                            JOptionPane.showMessageDialog(null, "Player two wins " + two.score + "/" + bestofNum + "!");
                            System.exit(0);
                        } else {
                            two.score = 0;
                        }
                        ball.reset();
                        JOptionPane.showMessageDialog(null, "Player two wins!");
                    }

                    long sleep = millisPs - (System.currentTimeMillis() - pre);
                    try {
                        Thread.sleep(Math.max(sleep, 0));
                    } catch (Exception e) {
                    }
                }

            }
        });

        showSetup();

        while (LoadingScreen) {
            try {
                Thread.yield();
            } catch (Exception e) {
            }
        }

        frame.setVisible(true);

        if (doArduino) {
            try {
                comm.initialize(ArdP);
            } catch (Exception e1) {
            }
        }

        if (p1AI) {
            P1AI_ref = new AI(one, ball, p1AIDif);
        }
        if (p2AI) {
            P2AI_ref = new AI(two, ball, p2AIDif);
        }

        thread.start();

        while (true) {

            try {

                graphics = buffer.getDrawGraphics();

                graphics.setColor(Color.BLACK);

                graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

                one.render(graphics, canvas);
                two.render(graphics, canvas);
                ball.render(graphics);

                graphics.setFont(SCOREFONT);

                graphics.drawString(one.score + "," + two.score, canvas.getWidth() / 2, 20);

                if (!buffer.contentsLost()) {
                    buffer.show();
                }

                Thread.yield();

            } finally {

                if (graphics != null) {
                    graphics.dispose();
                }

            }

        }

    }

}
