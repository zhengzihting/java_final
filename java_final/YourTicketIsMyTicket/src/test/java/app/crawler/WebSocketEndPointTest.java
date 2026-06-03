package app.crawler;

public class WebSocketEndPointTest{
    public static void main(String[] args){
        WebSocketEndPoint webSocketEndPoint = new WebSocketEndPoint();
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