/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.neno.models;

import com.neno.classes.ServerResponse;
import com.neno.networking.ServerNew;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author nedu
 */
public class Socket_c extends SocketRespondedListener{
    Socket mSocket;
    String mLastActivityStamp;
    PrintWriter mOut;
    BufferedReader mIn;
    Timer mTimer;
    TimerTask mCloseSocketTask;
    boolean mTimerIsSet;
    ArrayList<SocketRespondedListener> listeners ;
    private boolean isReading;
    private int mRequestID;
    
    
    public Socket_c(Socket socket){
        try {
            mSocket = socket;
            mOut = new PrintWriter(mSocket.getOutputStream(), true);
            mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            listeners = new ArrayList();
            mTimer = new Timer();
        } catch (IOException ex) {
            Logger.getLogger(Socket_c.class.getName()).log(Level.SEVERE, null, ex);
        }
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
                mCloseSocketTask = new CloseSocketTask();
                mTimer.schedule(mCloseSocketTask, timeout);
                mTimerIsSet = true;
            }
            
            if(!isReading){
                isReading = true;
                while((line = mIn.readLine()) != null){
                    if(mTimerIsSet){
                        mCloseSocketTask.cancel();
                        mTimer.purge();
                    }
                    
                    notifyOnRespondedListeners(line);
                }
                isReading = false;
            }
            
        } catch (IOException ex) {
            isReading = false;
            notifyOnRespondedListeners(ServerResponse.IsAliveResponse(ServerNew.StatusCodes.TIMEOUT).toString());
        } 
    }
    
    public Socket_c Send(JSONObject request){
        return send(request);
    }
    
    public Socket_c Send(JSONObject request, SocketRespondedListener l){
        try{
            String id = genRequestID();
            request.put(ServerNew.Keys.REQUEST_ID, id);
            l._requestID = id;
            l._once = true;
        }catch(JSONException ex){}
        
        addListener(l);
        return send(request);
        
    }
    
    private Socket_c send(JSONObject request){
        mOut.println(request);
        return this;
    }
    
    public Socket_c addListener(SocketRespondedListener l){
        l.bindSocket(this);
        listeners.add(l);
        return this;
    }
    
    private void notifyOnRespondedListeners(String response){
        boolean specific = false;
        String request_id = "";
        JSONObject jsonResponse = ServerResponse.ConnectionResponse(ServerNew.StatusCodes.NOT_IMPLEMENTED).toJSONObject();
        try{
            jsonResponse = new JSONObject(response);
            if(jsonResponse.has(ServerNew.Keys.REQUEST_ID)){
                specific = true;
                request_id = jsonResponse.getString(ServerNew.Keys.REQUEST_ID);
                System.out.println(response);
            }
        }catch(JSONException ex){ }
        
        Iterator<SocketRespondedListener> it = listeners.iterator();
        while(it.hasNext()){
            SocketRespondedListener l = it.next();
            if(specific){
                if(request_id.equals(l.getRequestID())){
                    l.postResponse(jsonResponse);
                    it.remove();
                    break;
                }
            }else   
                l.postResponse(jsonResponse);

            if(l.once()){
                it.remove();
            }
        }
        
       
    }
    
    public Socket socket(){
        return mSocket;
    }
    
    public void closeSocket(){
        try{
            mSocket.close();
        }catch(IOException ex){}
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
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}
