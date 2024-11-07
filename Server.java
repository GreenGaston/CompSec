import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;


public class Server {
    private final HashMap<String, Client> users;
    private final HashMap<String, Integer> counters;
    private ServerSocket serverSocket;
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public Server() {
        users = new HashMap<>();
        counters = new HashMap<>();
    
        try {
            serverSocket = new ServerSocket(9000);
    
            // Configure the logger to write to a file
            FileHandler fileHandler = new FileHandler("server.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void updateHashmap(String id, Boolean up) {
        if (up) {
            counters.put(id, counters.get(id) + 1);
        } else {
            counters.put(id, counters.get(id) - 1);
        }
        logger.log(Level.INFO, "Counter updated for user {0}: {1}", new Object[]{id, counters.get(id)});
    }

    public void start() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                new ServerThread(socket, this).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class Client {
        public String password;
        public String id;
        public Boolean loggedIn;

        public Client(String password, String id, Boolean loggedIn) {
            this.password = password;
            this.id = id;
            this.loggedIn = loggedIn;
        }

        public void increment() {
            updateHashmap(id, true);
        }

        public void decrement() {
            updateHashmap(id, false);
        }

        public void logout() {
            synchronized (Server.this) {
                users.remove(id);
                counters.remove(id);
            }
            loggedIn = false;
        }

        public void login() {
            loggedIn = true;
        }
    }

    public class ServerThread extends Thread {
        private final Socket socket;
        private final Server server;
        private String id;
        private BufferedReader in;
        private PrintWriter out;

        public ServerThread(Socket socket, Server server) {
            this.socket = socket;
            this.server = server;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                out.println("Enter your username and id");
                String line = in.readLine();
                String[] parts = line.split(" ");
                synchronized (server) {
                    if (server.users.containsKey(parts[0])) {
                        Client client = server.users.get(parts[0]);
                        if (client.loggedIn) {
                            out.println("You are already logged in");
                            return;
                        }
                        if (client.password.equals(parts[1])) {
                            client.login();
                            id = parts[0];
                            out.println("You are now logged in");
                        } else {
                            out.println("Incorrect password");
                            return;
                        }
                    } else {
                        out.println("Creating new user");
                        server.users.put(parts[0], server.new Client(parts[1], parts[0], true));
                        server.counters.put(parts[0], 0);
                        id = parts[0];
                    }
                }

                while (true) {
                    line = in.readLine();
                    if (line.equals("increment")) {
                        server.users.get(id).increment();
                        out.println("Counter is now: " + server.counters.get(id));
                    } else if (line.equals("decrement")) {
                        server.users.get(id).decrement();
                        out.println("Counter is now: " + server.counters.get(id));
                    } else if (line.equals("logout")) {
                        server.users.get(id).logout();
                        out.println("You are now logged out");
                        break;
                    } else {
                        out.println("Invalid command");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
