package app.crawler;

public class WebSocketEndPointTest{
    public static void main(String[] main){
        WebSocketEndPoint webSocketEndPoint = new WebSocketEndPoint();
        webSocketEndPoint.startWebSocket();
        try{
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}