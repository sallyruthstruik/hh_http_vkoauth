
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Станислав
 */

class TokenParseException extends Exception{
    private String message;
    
    public TokenParseException(String message){
        this.message = message;
    }
    
    public String toString(){
        return message;
    }
};

class IncorrectVKResponse extends TokenParseException{
    
    public IncorrectVKResponse(String m){
        super(m);
    }
};

class VKApi{
    private Integer SOCKET_TIMEOUT = 10000;     //10s
    private String access_token;
    private String client_id, permissions;
    private Integer port;
    
    private String getAuthUri(){
        return "https://oauth.vk.com/authorize?" + 
                "client_id="+client_id + "&" +
                "scope=" + permissions + "&" +
                "redirect_uri=http://localhost:"+port.toString()+"&"+
                "display=page&" +
                "response_type=token";
    }
    
    public VKApi(String client_id, String permissions, Integer port){
        System.out.println("Original constructor called");
        if(client_id == null || permissions == null || port == null){
            System.err.println("You can't pass null values");
            System.exit(1);
        }
        this.client_id = client_id;
        this.permissions = permissions;
        this.port = port;
        
        System.out.println("Use: client_id: "+ client_id + "\npermissions: "+ permissions + 
                "\nport: "+port + "\nSOCKET TIMEOUT: "+ SOCKET_TIMEOUT);
    }
    
    public VKApi(String client_id){
        this(client_id, "", 1234);
    }
    
    public void authorize(){
        openOauthUri();
        listenSocket();
    }
    
    //Открывает в браузере ссылку для авторизации
    private void openOauthUri(){
        String url = getAuthUri();
        
        try{
            System.out.println("Open URI: " + url);
            Desktop.getDesktop().browse(new URI(url));
        }catch(IOException e){
            System.err.println("Getted unexpected exception while try to open auth uri: "+e.toString());
            System.exit(2);
        }catch(URISyntaxException e){
            System.err.println("Syntax error in url "+url+" : "+e.toString());
            System.exit(3);
        }
    }
    
    private void listenSocket(){
        try(ServerSocket server = new ServerSocket(port)){
            server.setSoTimeout(SOCKET_TIMEOUT);
            System.out.println("Start to listen socket on port "+port.toString());
            
            while(true){
                Socket socket = server.accept();
                
                System.out.println("\n\nSocket accepted");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                
                String header = reader.readLine();
                System.out.println("Getted header: "+header);
                
                //Идет несколько тестовых запросов от контакта
                if(header.startsWith("GET / ")){
                    writer.write("<script>window.location = window.location.href.replace(\"#\", \"\")</script>");
                    writer.flush();
                }else{
                    //обрабатываем полученный access_token и выходим
                    try{
                        String token = prepareHeaderAndGetToken(header);
                        writer.write("Authorization successful: token is " + token);
                    }catch(Exception e){
                        writer.write("Authorization fails: "+e.toString());
                    }finally{
                        writer.flush();
                        socket.close();
                    }
                    break;
                }
                
                socket.close();
                
            }
            
        }catch(SocketTimeoutException e){
            System.err.println("Timeout while wait vk api request. To solve it change parameter SOCKET_TIMEOUT");
            System.exit(7);
        }catch(IOException e){
            System.err.println("Couldn't bind socket on port "+port.toString()+" : exception getted: "+e.toString());
            System.exit(4);
        }
        
    }
    
    public String prepareHeaderAndGetToken(String header)throws IncorrectVKResponse{
        System.out.println("Getted HTTP header with token: "+header);
        
        header = header.replaceFirst("/", "");
        
        Pattern pat = Pattern.compile(".*access_token=(\\w+).*");
        
        Matcher matcher = pat.matcher(header);
        
        if(!matcher.matches())
            throw new IncorrectVKResponse("Getted unexpected api response: "+header);
        
        access_token = matcher.group(1);
        
        return access_token;
        
    }
}

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
        VKApi api = new VKApi("3384572");
        api.authorize();
    }
    
}
