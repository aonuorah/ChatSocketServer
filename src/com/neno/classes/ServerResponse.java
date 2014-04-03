/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.neno.classes;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author nedu
 */
public class ServerResponse {
    public static final String ACTION_SEND_MESSAGE = "12";
    public static final String ACTION_CONTROL = "10";
    
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
    
    
    private final String CODE = "code";
    
    private JSONObject responseJSON;
    
    public ServerResponse(String code){
        responseJSON = new JSONObject();
        putOnce(CODE, code);
    }
    
    public ServerResponse put(String key, String[] values){
        for(String value : values){
            append(key, value);
        }
        return this;
    }
    
    public ServerResponse putOnce(String key, String value){
        try{
            responseJSON.putOnce(key, value);
        }catch(JSONException ex){
            System.out.println(ex.getMessage());
        }
        return this;
    }
    
    public ServerResponse put(String key, String value){
        try{
            responseJSON.put(key, value);
        }catch(JSONException ex){
            System.out.println(ex.getMessage());
        }
        return this;
    }
    
    public ServerResponse append(String key, String value){
        try{
            responseJSON.append(key, value);
        }catch(JSONException ex){
            System.out.println(ex.getMessage());
        }
        return this;
    }
    
    public boolean has(String key){
        return responseJSON.has(key);
    }
    
    @Override
    public String toString(){
        return responseJSON.toString() + "\r\n";
    }
}
