/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.neno.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nedu
 */
public class Socket_c extends SocketRespondedListener{
    Socket mSocket;
    String mLastActivityStamp;
    BufferedReader mIn;
    PrintWriter mOut;
    Timer checkAliveTimeoutTimer;
    ArrayList<SocketRespondedListener> listeners = new ArrayList();
    
    public Socket_c(Socket socket){
        try {
            mSocket = socket;
            mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            mOut = new PrintWriter(mSocket.getOutputStream(), true);
        } catch (IOException ex) {
            Logger.getLogger(Socket_c.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void read(){
        try {
            String line;
            while((line = mIn.readLine()) != null){
                notifyOnRespondedListeners(line);
            }
            
        } catch (IOException ex) {}
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
        Iterator<SocketRespondedListener> i = listeners.iterator();
        while(i.hasNext()){
            SocketRespondedListener l = i.next();
            l.postResponse(response);
            
            if(l.once())
                i.remove();
        }
    }
    
    public Socket socket(){
        return mSocket;
    }
    
    
}
