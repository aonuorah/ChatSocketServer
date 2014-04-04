package com.neno.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author nedu
 */
public class JServer {
    private ServerSocket listener;
    private static final int port = 9090;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)  {        
        JServer server = new JServer();
        
        try{
            server.startServer(port);
        }catch(IOException ex){
            System.out.println(ex.getMessage());
        }
    }
    
    private void startServer(int _port)throws IOException{
        try {
            listener = new ServerSocket(_port);
            while (true) {
                Socket socket = listener.accept();
                //Server connection = new Server(socket);
                ServerNew connection = new ServerNew(socket);
                new Thread(connection).start();
            }
        }finally{
            listener.close();
        }
        

    }
    
    
    
    
}
