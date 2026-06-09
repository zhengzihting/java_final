package app.crawler.websocket;

import java.io.File;
import java.io.IOException;

public class MacWebSocket implements WebSocketEndPoint {
    private int port;
    private String browserPath;
    private boolean haveBrowserPath = false;

    public MacWebSocket(){
        this.port = WebSocketEndPoint.port;
        defaultPath();
    }

    public MacWebSocket(int port){
        this.port = port;
        defaultPath();
    }

    public MacWebSocket(String browserPath){
        this.port = WebSocketEndPoint.port;
        this.browserPath = browserPath;
        haveBrowserPath = new File(this.browserPath).exists();
    }

    public MacWebSocket(int port, String browserPath){
        this.port = port;
        this.browserPath = browserPath;
        haveBrowserPath = new File(this.browserPath).exists();
    }

    public void defaultPath(){
        String macPath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
        this.browserPath = macPath;
        haveBrowserPath = new File(this.browserPath).exists();
    }

    public int getPort(){ return this.port; }

    public boolean browserPathExist(){ return this.haveBrowserPath; }

    public void startWebSocket() throws RuntimeException{
        if(haveBrowserPath){
            ProcessBuilder pb;
            pb = new ProcessBuilder(this.browserPath,
                    "--remote-debugging-port="+this.port,
                    "--user-data-dir=/tmp/chrome_debug",
                    "--no-first-run",                  // 跳過首次運行歡迎畫面
                    "--no-default-browser-check",      // 跳過預設瀏覽器檢查
                    "--use-mock-keychain");
            try{
                pb.start();
                Thread.sleep(1000);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}