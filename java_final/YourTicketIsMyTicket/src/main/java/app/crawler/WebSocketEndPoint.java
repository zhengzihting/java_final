package app.crawler;

import java.io.File;

public class WebSocketEndPoint {
    private int port;
    private String browserPath;
    private boolean haveBrowserPath = false;
    private String os = System.getProperty("os.name").toLowerCase();

    public WebSocketEndPoint(){
        this.port = 9222;
        defaultPath();
    }

    public WebSocketEndPoint(String browserPath){
        this.port = 9222;
        this.browserPath = browserPath;
        haveBrowserPath = new File(this.browserPath).exists();
    }

    private void defaultPath(){
        String macPath = "/Applications/Google\\ Chrome.app/Contents/MacOS/Google\\ Chrome";
        String[] winPaths = {"C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe"};
        if(os.contains("mac")){
            this.browserPath = macPath;
            haveBrowserPath = new File(this.browserPath).exists();
        }else if(os.contains("win")){
            for(String path : winPaths){
                this.browserPath = path;
                if(new File(this.browserPath).exists()){
                    haveBrowserPath = true;
                    break;
                }
            }
        }
    }

    public void startWebSocket(){
        if(haveBrowserPath){
            ProcessBuilder pb;
            if(os.contains("mac")){
                pb = new ProcessBuilder(String.format("%s --remote-debugging-port=%d --user-data-dir=\"/tmp/chrome_debug\"", this.browserPath, this.port));
            }else if(os.contains("win")){
                pb = new ProcessBuilder(String.format("%s --remote-debugging-port=%d --user-data-dir=\"C:\\ChromeProfile\"", this.browserPath, this.port));
            }
        }
    }
}