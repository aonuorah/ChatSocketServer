/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.neno.models;

import com.neno.classes.ServerResponse;
import com.neno.networking.Server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author nedu
 */
public class SocketObj extends SocketRespondedListener{
    private Socket mSocket;
    private CopyOnWriteArrayList<SocketRespondedListener> listeners ;
    private PrintWriter mOut;
    private BufferedReader mIn;
    private Timer mTimer;
    private TimerTask mCloseSocketTask;
    private boolean mTimerIsSet;
    private boolean isReading;
    private int mRequestID;
    
    
    public SocketObj(Socket socket){
        mSocket = socket;
        listeners = new CopyOnWriteArrayList();
    }
    
    @Override
    public void closeSocket(){
        try{
            mSocket.close();
        }catch(IOException ex){}
        
    }
    
    public void Read(){
        read(0);
    }
    
    
    public void Read(int timeout){
        read(timeout * 1000);
    }
    
    private void read(int timeout){
        String line;
        try {
            mTimerIsSet = false;
            if(timeout != 0){
                mTimer = new Timer();
                mCloseSocketTask = new CloseSocketTask();
                mTimer.schedule(mCloseSocketTask, timeout);
                mTimerIsSet = true;
            }
            
            if(!isReading){
                isReading = true;
                if(mIn == null){
                    mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                }
                while((line = mIn.readLine()) != null){
                    if(mTimerIsSet){
                        mCloseSocketTask.cancel();
                        mTimer.purge();
                        mTimer.cancel();
                    }
                    
                    notifyOnRespondedListeners(line);
                }
                isReading = false;
                notifyOnRespondedListeners(ServerResponse.IsAliveResponse(Server.StatusCodes.GONE).toString());
            }
            
        } catch (IOException ex) {
            isReading = false;
            notifyOnRespondedListeners(ServerResponse.IsAliveResponse(Server.StatusCodes.TIMEOUT).toString());
        } 
    }
    
    public SocketObj Send(JSONObject request){
        return send(request);
    }
    
    public SocketObj Send(JSONObject request, SocketRespondedListener l){
        try{
            String id = genRequestID();
            request.put(Server.Keys.REQUEST_ID, id);
            l._requestID = id;
            l._once = true;
        }catch(JSONException ex){}
        
        addListener(l);
        return send(request);
        
    }
    
    private SocketObj send(JSONObject request){
        if(mOut == null){
            try {
                mOut = new PrintWriter(mSocket.getOutputStream(), true);
            } catch (IOException ex) {}
        }
        mOut.println(request);
        return this;
    }
    
    public SocketObj addListener(SocketRespondedListener l){
        l.bindSocket(this);
        listeners.add(l);
        return this;
    }
    
    private void notifyOnRespondedListeners(String response){
        boolean specific = false;
        String request_id = "";
        JSONObject jsonResponse = ServerResponse.ConnectionResponse(Server.StatusCodes.NOT_IMPLEMENTED).toJSONObject();
        try{
            jsonResponse = new JSONObject(response);
            if(jsonResponse.has(Server.Keys.REQUEST_ID)){
                specific = true;
                request_id = jsonResponse.getString(Server.Keys.REQUEST_ID);
            }
        }catch(JSONException ex){ }
        
        Iterator<SocketRespondedListener> it = listeners.iterator();
        while(it.hasNext()){
            SocketRespondedListener l = it.next();
            if(specific){
                if(request_id.equals(l.getRequestID())){
                    l.postResponse(jsonResponse);
                    listeners.remove(l);
                    break;
                }
            }else {  
                l.postResponse(jsonResponse);
                if(l.once()){
                    listeners.remove(l);
                }
            }
        }
    }
    
    public Socket socket(){
        return mSocket;
    }
    
    
    private String genRequestID(){
        return String.valueOf(mRequestID++);
    }
    
    private class CloseSocketTask extends TimerTask{
        @Override
        public void run(){
            try {
                mSocket.close();
                this.cancel();
                mTimer.purge();
                mTimer.cancel();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}
