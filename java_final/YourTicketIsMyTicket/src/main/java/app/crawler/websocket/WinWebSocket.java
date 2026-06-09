package app.crawler.websocket;

import java.io.File;
import java.io.IOException;

public class WinWebSocket implements WebSocketEndPoint{
    private int port;
    private String browserPath;
    private boolean haveBrowserPath = false;

    public WinWebSocket(){
        this.port = WebSocketEndPoint.port;
        defaultPath();
    }

    public WinWebSocket(int port){
        this.port = port;
        defaultPath();
    }

    public WinWebSocket(String browserPath){
        this.port = WebSocketEndPoint.port;
        this.browserPath = browserPath;
        haveBrowserPath = new File(this.browserPath).exists();
    }

    public WinWebSocket(int port, String browserPath){
        this.port = port;
        this.browserPath = browserPath;
        haveBrowserPath = new File(this.browserPath).exists();
    }

    public void defaultPath(){
        String[] winPaths = {"C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe"};

        for(String path : winPaths){
            this.browserPath = path;
            if(new File(this.browserPath).exists()){
                haveBrowserPath = true;
                break;
            }
        }
    }

    public int getPort(){ return this.port; }

    public boolean browserPathExist(){ return this.haveBrowserPath; }

    public void startWebSocket() throws RuntimeException{
        if(haveBrowserPath){
            ProcessBuilder pb;
            pb = new ProcessBuilder(this.browserPath,
                    "--remote-debugging-port="+this.port,
                    "--user-data-dir=C:\\ChromeProfile",
                    "--no-first-run",
                    "--no-default-browser-check");
            try{
                pb.start();
                Thread.sleep(1000);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}