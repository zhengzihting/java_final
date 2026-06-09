package app.crawler.websocket;

public interface WebSocketEndPoint {
    int port = 9222;
    void defaultPath();
    int getPort();
    boolean browserPathExist();
    void startWebSocket() throws RuntimeException;
}