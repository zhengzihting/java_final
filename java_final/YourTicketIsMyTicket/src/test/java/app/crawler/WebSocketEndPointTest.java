package app.crawler;

import app.crawler.websocket.MacWebSocket;
import app.crawler.websocket.WebSocketEndPoint;
import app.crawler.websocket.WinWebSocket;

public class WebSocketEndPointTest{
    public static void main(String[] args){
        String os = System.getProperty("os.name").toLowerCase();
        WebSocketEndPoint webSocketEndPoint;
        if(os.contains("mac")) webSocketEndPoint = new MacWebSocket();
        else if(os.contains("win")) webSocketEndPoint = new WinWebSocket();
        else{
            System.out.println("No support.");
            return;
        }
        try{
            webSocketEndPoint.startWebSocket();
            Thread.sleep(3000);
        }catch(RuntimeException e){
            System.err.println(e);
            throw e;
        }catch(InterruptedException e) {
            System.err.println(e);
        }
    }
}