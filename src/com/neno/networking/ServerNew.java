/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.neno.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import com.neno.classes.ServerResponse;
import com.neno.models.Socket_c;
import com.neno.models.SocketRespondedListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author nedu
 */
public class ServerNew implements Runnable{
    private final static HashMap<String, Socket_c> mSockets = new HashMap();
    private static HashMap<String, Socket> activeSockets = new HashMap();
    private static Socket_c mSocket;
    Timer checkAliveTimeoutTimer;
    
    public ServerNew(Socket _socket){
        mSocket = new Socket_c(_socket);
    }
    
    @Override
    public void run() {
        try{
            init2();
        }catch(IOException ex){
            System.out.println(ex.getMessage());
        }
    }
    
    private void init2()throws IOException{
        System.out.println("Connection received from " + mSocket.socket().getInetAddress());
        
        mSocket.addListener(new SocketRespondedListener(true){
            @Override
            public void postResponse(JSONObject response){
                super.postResponse(response);
                processConnectionRequest(this, response);
            }
        }).Read();
    }
    /*
    {"code":"11","name":"a"}
    {"code":"909","status":"200"}
    {"code":"909","status":"400"}
    */
    private void processConnectionRequest(final SocketRespondedListener l, JSONObject response){
        boolean closeSocket = true;
        try {
            if(response.getString(Keys.CODE).equals(RequestCodes.CONNECT)){
                final String name = response.getString(Keys.NAME).trim();
                if(!name.isEmpty()){
                    closeSocket = false;
                    if(!mSockets.containsKey(name)){
                        acceptConnection(name);
                    }else{
                        mSockets.get(name).Send(ServerResponse.IsAliveRequest().toJSONObject(), 
                                                new SocketRespondedListener(){
                                                    
                                                        @Override
                                                        public void postResponse(JSONObject response2){
                                                            super.postResponse(response2);
                                                            try {
                                                                if(response2.getString(Keys.CODE).equals(ResponseCodes.IS_ALIVE)){
                                                                    if(response2.getString(Keys.STATUS).equals(StatusCodes.SUCCESS)){
                                                                        rejectConnection(StatusCodes.UNAUTHORIZED);

                                                                    }else{
                                                                        this.closeSocket();
                                                                        acceptConnection(name);
                                                                    }
                                                                }
                                                            } catch (JSONException ex) {
                                                                Logger.getLogger(ServerNew.class.getName()).log(Level.SEVERE, null, ex);
                                                            }
                                                        }
                                                    }).Read(3);
                    }
                }
            }
        } catch (JSONException ex) {}
        finally{
            if(closeSocket){
                rejectSocket(response);
            }
        }
    }
    
    private void acceptConnection(String name){
        mSockets.put(name, mSocket);
        mSocket.Send(ServerResponse.ConnectionResponse(StatusCodes.ACCEPTED).toJSONObject());
    }
    
    private void rejectConnection(String code){
        mSocket.Send(ServerResponse.ConnectionResponse(code).toJSONObject());
        mSocket.closeSocket();
    }
    
    private void rejectSocket(JSONObject response){
        mSocket.Send(response);
        mSocket.closeSocket();
    }
    
    private void init(Socket _socket)throws IOException{
        /*ServerResponse response = new ServerResponse(ServerResponse.CONNECTION_REQUEST);;
        System.out.println("Connection received from " + socket.getInetAddress());
            
        String[] command;
        String socket_id = "";
        boolean connectionIsValid = false;
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        
        try {
            String commandLine = in.readLine();
            System.out.println(commandLine);
            command = commandLine.split(" ");
            
            if(command.length >= 2 && command[0].equals("connect") ){
                socket_id = command[1];
                if(!socket_id.trim().isEmpty()){
                    connectionIsValid = true;
                    if(activeSockets.containsKey(socket_id)){
                        if(command.length >= 3 && command[2].equals("force")){
                            activeSockets.get(socket_id).close();
                            activeSockets.remove(socket_id);
                        }else{
                            isAlive(activeSockets.remove(socket_id));
                            connectionIsValid = false;
                            response.put("status", ServerResponse.UNAUTHORIZED);
                        }
                    }
                    
                    if(connectionIsValid){
                        response.put("status", ServerResponse.SUCCESS);
                        for(Map.Entry<String, Socket> s : activeSockets.entrySet()){
                            response.append("online", s.getKey());//append list of online users to json
                        }
                        activeSockets.put(socket_id, socket);
                    }
                }
            }
            if(!response.has("status")){
                response.put("status", ServerResponse.NOT_IMPLEMENTED);
            }else{
                response.put("active", String.valueOf(activeSockets.size()));
            }
            
            out.println(response);
            System.out.println(response);
        }catch(NullPointerException ex){
            socket.close();
            System.out.println("Connection " + socket.getInetAddress() + "disconnected!");
        }finally {
            if(!connectionIsValid){
                socket.close();
            }else{
                putOnline(socket, socket_id);
            }
        }*/
    }
    
    private void putOnline(Socket _socket, String socket_id){
        this.notifyUserOnline(socket_id);
        
        String line;
        String receiver;
        String message;
        String stamp;
        ServerResponse response = null;
        
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
            PrintWriter out = new PrintWriter(_socket.getOutputStream(), true);
            String receivedText = "";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
            Calendar calendar = Calendar.getInstance();
            while((line = in.readLine()) != null){
                receivedText += line;
                if(!in.ready()){
                    try{
                        JSONObject j = new JSONObject(receivedText);
                        String action = j.getString("code");
                        switch(action){
                            case ServerResponse.ACTION_CONTROL:
                                break;
                                
                            case ServerResponse.ACTION_SEND_MESSAGE:
                                
                                    receivedText = "";
                                    receiver = j.getString("to");
                                    message = j.getString("message");
                                    if(!j.has("stamp")){
                                        stamp = sdf.format(calendar.getTime());
                                    }else{
                                        try{
                                        stamp = sdf.format(sdf.parse(j.getString("stamp")));
                                        }catch(ParseException ex){
                                            System.out.println(ex.getMessage());
                                            stamp = sdf.format(calendar.getTime());
                                        }
                                    }
                                    if(activeSockets.containsKey(receiver)){
                                        Socket s = activeSockets.get(receiver);
                                        if(!s.isClosed()){
                                            PrintWriter r_out = new PrintWriter(s.getOutputStream(), true);
                                            r_out.println(new ServerResponse(ServerResponse.NEW_MESSAGE).put("from",socket_id).put("message",message).put("stamp", stamp));
                                            response = new ServerResponse(ServerResponse.SUCCESS);
                                        }else{
                                            activeSockets.remove(receiver);
                                        }
                                    }
                                break;
                                
                            default:
                                break;
                        }
                        
                    }catch(JSONException ex){
                        System.out.println(ex.getMessage());
                        response = new ServerResponse(ServerResponse.BAD_REQUEST);
                    }
                    
                    if(response == null){
                        response = new ServerResponse(ServerResponse.NOT_FOUND);
                    }
                    out.println(response);
                    response = null;
                }
                
            }
            _socket.close();
            notifyUserOffline(socket_id);
            
        }catch(IOException ex){
            System.out.println(ex.getMessage());
        }
    }
    
    private void notifyUserOnline(String socket_id){
        for(Map.Entry<String, Socket> _socket : activeSockets.entrySet()){
            if(!_socket.getKey().equals(socket_id)){
                try{
                    PrintWriter r_out = new PrintWriter(_socket.getValue().getOutputStream(), true);
                    r_out.println(new ServerResponse(ServerResponse.USER_UPDATE_ONLINE).put("id", socket_id));
                }catch(IOException ex){
                    System.out.println(ex.getMessage());
                }
            }
        }
    }
    
    private void notifyUserOffline(String socket_id){
        activeSockets.remove(socket_id);
        for(Map.Entry<String, Socket> _socket : activeSockets.entrySet()){
            try{
                PrintWriter r_out = new PrintWriter(_socket.getValue().getOutputStream(), true);
                r_out.println(new ServerResponse(ServerResponse.USER_UPDATE_OFFLINE).put("id", socket_id));
            }catch(IOException ex){
                System.out.println(ex.getMessage());
            }
        }
        System.out.println(socket_id + " disconnected from server");
    }
    
    private boolean isAlive(Socket s){
        
        return true;
    }
    
    
    public class RequestCodes{
        
        public static final String SEND_MESSAGE = "12";
        public static final String CONTROL = "10";
        public static final String CONNECT = "11";
    }
    
    public class ResponseCodes{
        public static final String NEW_MESSAGE = "902";
        public static final String SENT_MESSAGE_UPDATE = "903";
        public static final String USER_UPDATE_ONLINE = "904";
        public static final String USER_UPDATE_OFFLINE = "905";
        public static final String IS_ALIVE = "909";
    }
    
    public class StatusCodes{
        public static final String SUCCESS = "200";
        public static final String ACCEPTED = "202";
        public static final String NOT_IMPLEMENTED = "501";
        public static final String NOT_FOUND = "404";
        public static final String UNAUTHORIZED = "401";
        public static final String BAD_REQUEST = "400";
        public static final String TIMEOUT = "408";
    }
    
    public class Keys{
        public static final String CODE = "code";
        public static final String STATUS = "status";
        public static final String NAME = "name";
        public static final String REQUEST_ID = "request_id";
    }
    
}