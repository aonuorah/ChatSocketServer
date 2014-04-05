/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.neno.classes;

import com.neno.networking.ServerNew;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author nedu
 */
public class ServerResponse {
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
    
    
    private final String CODE = "code";
    
    private JSONObject responseJSON;
    
    public ServerResponse(String code){
        responseJSON = new JSONObject();
        putOnce(CODE, code);
    }
    
    public static ServerResponse ConnectionResponse(String status){
        return new ServerResponse(ServerNew.RequestCodes.CONNECT).put(ServerNew.Keys.STATUS, status);
    }
    
    public static ServerResponse IsAliveRequest(){
        return new ServerResponse(ServerNew.ResponseCodes.IS_ALIVE);
    }
    
    public static ServerResponse IsAliveResponse(String status){
        return new ServerResponse(ServerNew.ResponseCodes.IS_ALIVE).put(ServerNew.Keys.STATUS, status);
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
    
    public JSONObject toJSONObject(){
        return responseJSON;
    }
        
    @Override
    public String toString(){
        return responseJSON.toString();
    }
}
