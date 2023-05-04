import java.io.IOException;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;


public class ClientHandler {
    private MyServer myServer;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String name;
    public String getName(){
        return name;
    }
    public ClientHandler(MyServer myServer,Socket socket){
        try {
            this.myServer=myServer;
            this.socket=socket;
            this.in=new DataInputStream(socket.getInputStream());
            this.out=new DataOutputStream(socket.getOutputStream());
            this.name="";
            new Thread(()->{
                try {
                    authentication();
                }catch (IOException e){
                    e.printStackTrace();
                }finally {
                    closeConnection();
                }
            }).start();
        }catch (IOException e){
            throw new RuntimeException("проблемы при сохдании обработчика клиента");
        }
    }
    public void authentication() throws IOException{
        while (true){
            String str=in.readUTF();
            if (str.startsWith("/auth")){
                String[] parts=str.split("\\s");
                String nick =myServer.getAuthService().getNickByLoginPass(parts[1],parts[2]);
                if (nick!=null){
                    if (!myServer.isNickBusy(nick)){
                        sendMsg("/authok" +nick);
                        name=nick;
                        myServer.broadcastMsg(name+"зашел в чат");
                        myServer.subscrube(this);
                        return;
                    }else {
                        sendMsg("учетная запись уже используеться");
                    }
                }else {
                    sendMsg("неверный логин/пароль");
                }
            }
        }
    }
    public void readMessages()throws IOException{
        while (true){
            String stfFromClient=in.readUTF();
            System.out.println("от"+name+": "+stfFromClient);
            if (stfFromClient.equals("/end")){
                return;
            }
            myServer.broadcastMsg(name+": "+stfFromClient);
        }
    }
    public void sendMsg(String msg){
        try {
            out.writeUTF(msg);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public void closeConnection(){
        myServer.unsubscrube(this);
        myServer.broadcastMsg(name+" вышел из чата");
        try {
            in.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        try {
            out.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        try {
            socket.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
