package app.crawler.websocket;

import java.io.File;
import java.io.IOException;

public interface WebSocketEndPoint {
    int port = 9222;
    void defaultPath();
    int getPort();
    boolean browserPathExist();
    void startWebSocket() throws RuntimeException;
}