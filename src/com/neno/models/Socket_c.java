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

/**
 *
 * @author nedu
 */
public class Socket_c extends SocketRespondedListener{
    Socket mSocket;
    String mLastActivityStamp;
    PrintWriter mOut;
    BufferedReader mIn;
    Timer checkAliveTimeoutTimer;
    ArrayList<SocketRespondedListener> listeners = new ArrayList();
    private boolean isReading;
    Timer mTimer;
    
    public Socket_c(Socket socket){
        try {
            mSocket = socket;
            mOut = new PrintWriter(mSocket.getOutputStream(), true);
            mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
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
            if(timeout != 0){
                mTimer = new Timer();
                mTimer.schedule(new CloseSocketTask(), timeout);
            }
            
            if(!isReading){
                isReading = true;
                while((line = mIn.readLine()) != null){
                    try{
                        mTimer.cancel();
                    }catch(NullPointerException ex){}
                    
                    notifyOnRespondedListeners(line);
                }
            }
            
        } catch (IOException ex) {
            isReading = false;
            notifyOnRespondedListeners(ServerResponse.IsAliveResponse(ServerNew.StatusCodes.TIMEOUT).toString());
        } finally{
            isReading = false;
        }
    }
    
    public Socket_c Send(String request){
        return send(request);
    }
    
    public Socket_c Send(String request, SocketRespondedListener l){
        addListener(l);
        return send(request);
        
    }
    
    private Socket_c send(String request){
        mOut.println(request);
        return this;
    }
    
    public Socket_c addListener(SocketRespondedListener l){
        l.bindSocket(mOut);
        listeners.add(l);
        return this;
    }
    
    private void notifyOnRespondedListeners(String response){
        ArrayList<Integer> remove = new ArrayList();
        System.out.println("Start: "+listeners.size());
        
        for(int i = 0; i < listeners.size(); i++){
            listeners.get(i).postResponse(response);
            if(listeners.get(i).once())
                remove.add(Integer.valueOf(i));
        }
        System.out.println("Removing: "+remove.size());
        
        for(int i = 0; i < remove.size(); i++){
            listeners.remove(remove.get(i).intValue());
        }
        
        System.out.println("End: "+listeners.size());
        
    }
    
    public Socket socket(){
        return mSocket;
    }
    
    private class CloseSocketTask extends TimerTask{
        @Override
        public void run(){
            try {
                mSocket.close();
                mTimer.cancel();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}
