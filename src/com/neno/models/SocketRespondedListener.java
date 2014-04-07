/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.neno.models;

import java.io.IOException;
import org.json.JSONObject;

/**
 *
 * @author nedu
 */
public class SocketRespondedListener{
    protected boolean _once;
    protected String _requestID;
    private SocketObj _socket;

    public SocketRespondedListener(){
        this._once = false;
    }

    public SocketRespondedListener(boolean once){
        this._once = once;
    }

    public void postResponse(JSONObject response){

    }
    
    public boolean once(){
        return this._once;
    }

    public String getRequestID(){
        return _requestID;
    }
    
    public void closeSocket(){
        try{
            _socket.socket().close();
        }catch(IOException ex){}
        
    }
    
    protected void bindSocket(SocketObj socket){
        _socket = socket;
    }
    
    
    
}