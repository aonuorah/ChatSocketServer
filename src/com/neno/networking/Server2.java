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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Timer;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author nedu
 */
@Deprecated
public class Server2 implements Runnable{
    private static HashMap<String, Socket> activeSockets = new HashMap();
    private Socket socket;
    Timer checkAliveTimeoutTimer;
    
    public Server2(Socket _socket){
        socket = _socket;
    }
    
    @Override
    public void run() {
        try{
            init(socket);
        }catch(IOException ex){
            System.out.println(ex.getMessage());
        }
    }
    
    private void init(Socket _socket)throws IOException{
        ServerResponse response = new ServerResponse(Response.CONNECTION_REQUEST);;
        System.out.println("Connection received from " + socket.getInetAddress());
            
        String[] command;
        String socket_id = "";
        boolean connectionIsValid = false;
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));;
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
                            response.put("status", Response.UNAUTHORIZED);
                        }
                    }
                    
                    if(connectionIsValid){
                        response.put("status", Response.SUCCESS);
                        for(Map.Entry<String, Socket> s : activeSockets.entrySet()){
                            response.append("online", s.getKey());//append list of online users to json
                        }
                        activeSockets.put(socket_id, socket);
                    }
                }
            }
            if(!response.has("status")){
                response.put("status", Response.NOT_IMPLEMENTED);
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
        }
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
                            case Response.ACTION_CONTROL:
                                break;
                                
                            case Response.ACTION_SEND_MESSAGE:
                                
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
                                            r_out.println(new ServerResponse(Response.NEW_MESSAGE).put("from",socket_id).put("message",message).put("stamp", stamp));
                                            response = new ServerResponse(Response.SUCCESS);
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
                        response = new ServerResponse(Response.BAD_REQUEST);
                    }
                    
                    if(response == null){
                        response = new ServerResponse(Response.NOT_FOUND);
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
                    r_out.println(new ServerResponse(Response.USER_UPDATE_ONLINE).put("id", socket_id));
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
                r_out.println(new ServerResponse(Response.USER_UPDATE_OFFLINE).put("id", socket_id));
            }catch(IOException ex){
                System.out.println(ex.getMessage());
            }
        }
        System.out.println(socket_id + " disconnected from server");
    }
    
    private boolean isAlive(Socket s){
        /*PrintWriter r_out;
        try {
            r_out = new PrintWriter(s.getOutputStream(), true);
            r_out.println(new ServerResponse(Response.IS_ALIVE));
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String line, response = "";
            while((line = in.readLine()) != null){
                response += line;
                if(!in.ready()){
                    JSONObject r = new JSONObject(response);
                    if(r.getString("code").equals(Response.IS_ALIVE))
                }
            }
        } catch (IOException | JSONException ex) {
            System.out.println(ex.getMessage());
        }
        
        checkAliveTimeoutTimer = new Timer();
        checkAliveTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
              // Your database code here
            }
        }, 5*1000);
        */
        return true;
    }
    
    private class Response{
        public static final String ACTION_SEND_MESSAGE = "12";
        public static final String ACTION_CONTROL = "10";
        public static final String ACTION_CONNECT = "11";

        public static final String SUCCESS = "200";
        public static final String NOT_IMPLEMENTED = "501";
        public static final String NOT_FOUND = "404";
        public static final String UNAUTHORIZED = "401";
        public static final String BAD_REQUEST = "400";

        public static final String CONNECTION_REQUEST = "901";
        public static final String NEW_MESSAGE = "902";
        public static final String SENT_MESSAGE_UPDATE = "903";
        public static final String USER_UPDATE_ONLINE = "904";
        public static final String USER_UPDATE_OFFLINE = "905";
        public static final String IS_ALIVE = "909";
    }
    
}