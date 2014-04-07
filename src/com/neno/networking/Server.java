/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.neno.networking;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import com.neno.classes.ServerResponse;
import com.neno.models.SocketObj;
import com.neno.models.SocketRespondedListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author nedu
 */
public class Server implements Runnable{
    private final static HashMap<String, SocketObj> mSockets = new HashMap();
    private static SocketObj mSocket;
    
    public Server(Socket _socket){
        mSocket = new SocketObj(_socket);
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
                                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                }).Read(5);
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
    
    private void acceptConnection(final String name){
        ServerResponse response = ServerResponse.ConnectionResponse(StatusCodes.ACCEPTED);
        for(Map.Entry<String, SocketObj> sc : mSockets.entrySet()){
            if(!sc.getKey().equals(name)){
                response.append(Keys.ONLINE, sc.getKey());//append list of online users to json
            }
        }
        
        mSockets.put(name, mSocket);
        mSocket.addListener(new SocketRespondedListener(){
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
            Calendar calendar = Calendar.getInstance();
            String timestamp;
            @Override
            public void postResponse(JSONObject response){

                try{
                    switch(response.getString(Keys.CODE)){
                        case ResponseCodes.IS_ALIVE:
                            
                            if(response.getString(Keys.STATUS).equals(StatusCodes.GONE) || response.getString(Keys.STATUS).equals(StatusCodes.TIMEOUT) ){
                                this.closeSocket();
                                notifyUserOffline(name);
                            }
                            break;
                            
                        case RequestCodes.CONTROL:
                            break;

                        case RequestCodes.SEND_MESSAGE:

                                String receiver = response.getString(Keys.TO);
                                final String message_id = response.getString(Keys.ID);
                                if(!response.has(Keys.TIMESTAMP)){
                                    timestamp = dateFormat.format(calendar.getTime());
                                }else{
                                    try{
                                    timestamp = dateFormat.format(dateFormat.parse(response.getString(Keys.TIMESTAMP)));
                                    }catch(ParseException ex){
                                        System.out.println(ex.getMessage());
                                        timestamp = dateFormat.format(calendar.getTime());
                                    }
                                }
                                boolean found = false;
                                if(mSockets.containsKey(receiver)){
                                    SocketObj s = mSockets.get(receiver);
                                    if(!s.socket().isClosed()){
                                        found = true;
                                        s.Send(new ServerResponse(ResponseCodes.NEW_MESSAGE)
                                                .put(Keys.FROM, name)
                                                .put(Keys.MESSAGE,response.getString(Keys.MESSAGE))
                                                .put(Keys.TIMESTAMP, timestamp).toJSONObject(),
                                                new SocketRespondedListener(){
                                                    @Override
                                                    public void postResponse(JSONObject response2){
                                                        try{
                                                        mSocket.Send(
                                                                    ServerResponse.MessageDelivery(response2.getString(Keys.STATUS))
                                                                    .put(Keys.ID, message_id).toJSONObject()
                                                                );
                                                        }catch(JSONException ex){}
                                                    }
                                                }
                                        ).Read();
                                    }
                                }
                                if(!found){
                                    mSockets.remove(receiver);
                                    mSocket.Send(
                                                ServerResponse.MessageDelivery(StatusCodes.NOT_FOUND)
                                                .put(Keys.ID, message_id).toJSONObject()
                                            );
                                }
                            break;

                        default:
                            break;
                    }
                }catch(JSONException ex){System.out.println(ex.getMessage());}
            }
        }).Send(response.toJSONObject())
        .Read();
        notifyUserOnline(name);
        System.out.println(name + " is logged in");        
    }
    
    private void rejectConnection(String code){
        mSocket.Send(ServerResponse.ConnectionResponse(code).toJSONObject());
        mSocket.closeSocket();
    }
    
    private void rejectSocket(JSONObject response){
        mSocket.Send(response);
        mSocket.closeSocket();
    }
    
    
    private void notifyUserOnline(String name){
        for(Map.Entry<String, SocketObj> socket : mSockets.entrySet()){
            if(!socket.getKey().equals(name)){
                socket.getValue().Send(new ServerResponse(ResponseCodes.USER_UPDATE_ONLINE).put(Keys.NAME, name).toJSONObject());
            }
        }
    }
    
    private void notifyUserOffline(String name){
        mSockets.remove(name);
        for(Map.Entry<String, SocketObj> socket : mSockets.entrySet()){
            socket.getValue().Send(new ServerResponse(ResponseCodes.USER_UPDATE_OFFLINE).put(Keys.NAME, name).toJSONObject());
        }
        System.out.println(name + " is logged out");
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
        public static final String GONE = "410";
    }
    
    public class Keys{
        public static final String CODE = "code";
        public static final String STATUS = "status";
        public static final String NAME = "name";
        public static final String ID = "id";
        public static final String REQUEST_ID = "request_id";
        public static final String ONLINE = "online";
        public static final String TIMESTAMP = "stamp";
        public static final String TO = "to";
        public static final String FROM = "from";
        public static final String MESSAGE = "message";
    }
    
}