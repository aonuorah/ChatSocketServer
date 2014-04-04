/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.neno.models;

import java.io.PrintWriter;

/**
 *
 * @author nedu
 */
public class SocketRespondedListener{
    private boolean _once;
    private int _id;
    private PrintWriter _out;

    public SocketRespondedListener(){

    }

    public SocketRespondedListener(boolean once){
        this._once = once;
    }

    public void postResponse(String response){

    }
    
    public void postReply(String reply){
        _out.println(reply);
    }

    public boolean once(){
        return this._once;
    }

    public int getID(){
        return _id;
    }
    
    protected void bindSocket(PrintWriter out){
        _out = out;
    }

}