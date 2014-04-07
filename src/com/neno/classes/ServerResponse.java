/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.neno.classes;

import com.neno.networking.Server;
import com.neno.networking.Server.Keys;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author nedu
 */
public class ServerResponse {
    
    private JSONObject responseJSON;
    
    public ServerResponse(String code){
        responseJSON = new JSONObject();
        putOnce(Keys.CODE, code);
    }
    
    public static ServerResponse ConnectionResponse(String status){
        return new ServerResponse(Server.RequestCodes.CONNECT).put(Server.Keys.STATUS, status);
    }
    
    public static ServerResponse IsAliveRequest(){
        return new ServerResponse(Server.ResponseCodes.IS_ALIVE);
    }
    
    public static ServerResponse IsAliveResponse(String status){
        return new ServerResponse(Server.ResponseCodes.IS_ALIVE).put(Server.Keys.STATUS, status);
    }
    
    public static ServerResponse MessageDelivery(String status){
        return new ServerResponse(Server.ResponseCodes.SENT_MESSAGE_UPDATE).put(Server.Keys.STATUS, status);
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
