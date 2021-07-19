package com.patch.patchcalling.utils;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Created by Shivam Sharma on 25-07-2019.
 */
public class SocketIOManager {
    private static Socket socket;
    private static Boolean isNewInstance = true;
    private static Boolean isUnAuthorized = false;

    private SocketIOManager() {
    }

    public static Socket getSocket(IO.Options options, String url) {
        if (socket == null) {
            try {
                socket = IO.socket(url, options);
                isNewInstance = true;
            } catch (Exception e) {
                //throw new RuntimeException(e);
            }
        }else {
            isNewInstance = false;
        }
        //Log.d("PatchSocketObject", String.valueOf("Socjet objet : "+socket));
        return socket;
    }

    public static void setSocket(Socket socket) {
        SocketIOManager.socket = socket;
    }

    public static Socket getSocket() {
        return socket;
    }

    public static Boolean getIsNewInstance() {
        return isNewInstance;
    }

    public static void setSocketInstanceNull(){
        if(socket!=null){
            socket.close();
            socket.off();
            socket = null;
        }
    }

    public static Boolean isSocketConnected(){
        if(socket!=null){
            return socket.connected();
        }else {
            return false;
        }
    }

    public static Boolean getIsUnAuthorized() {
        return isUnAuthorized;
    }

    public static void setIsUnAuthorized(Boolean isUnAuthorized) {
        SocketIOManager.isUnAuthorized = isUnAuthorized;
    }
}
