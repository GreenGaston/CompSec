import java.io.*;
import java.net.*;
//for reading json file
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Client {
    private String password;
    private String id;
    private String port;
    private String serverAddress;
    private Socket socket;


    public Client() {
        //empty constructor
    }
    public Client(JSONObject json) {
        
        this.password = (String) json.get("password");
        this.id = (String) json.get("id");
        JSONObject server = (JSONObject) json.get("server");
        this.port =String.valueOf(server.get("port"));
        this.serverAddress = (String) server.get("address");

        
    }
    public void connect() {
        try {
            socket = new Socket("localhost", Integer.parseInt(port));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(id+" "+password);
            while(!in.ready()){
                Thread.sleep(100);
            }
            String response = in.readLine();
            out.println(id+" "+password);
            response = in.readLine();
            if("You are now logged in".equals(response)||"Creating new user".equals(response)){
                System.out.println(response);
                while(true){
                    out.println("increment");
                    response = in.readLine();
                    Thread.sleep(1000);
                }
            }else{
                System.out.println(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String args[]) throws IOException 
    {
        //start the server
        serverThread server = new serverThread();
        server.start();
        //import json named "client.json"
        JSONParser parser = new JSONParser();
        JSONObject json=null;
        try {
            Object obj = parser.parse(new FileReader("client.json"));
            json = (JSONObject) obj;
           
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(json);
        Client client = new Client(json);
        client.connect();
            

    }

    public static class serverThread extends Thread{
        //thread that keeps the server running
        public void run(){
            Server server = new Server();
            server.start();
        }

    }
}