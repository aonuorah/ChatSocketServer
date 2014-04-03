/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.neno.models;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;

/**
 *
 * @author nedu
 */
public class Socket_c {
    Socket mSocket;
    String mLastActivityStamp;
    BufferedReader in;
    PrintWriter out;
    Timer checkAliveTimeoutTimer;
    ArrayList<SocketResponedListener> listeners = new ArrayList();
    
    
    
    public void send(String string){
        
    }
    
    public void addListener(SocketResponedListener l){
        listeners.add(l);
    }
    
    public void notifyOnRespondedListeners(){
        for(SocketResponedListener l:listeners){
            l.postResponse();
        }
    }
    
    public interface SocketResponedListener{
        public void postResponse();
    }
}
