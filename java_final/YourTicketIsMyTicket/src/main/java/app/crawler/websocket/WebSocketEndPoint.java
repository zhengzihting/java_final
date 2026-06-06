package app.crawler.websocket;

import java.io.File;
import java.io.IOException;

public class WebSocketEndPoint {
    private int port;
    private String browserPath;
    private boolean haveBrowserPath = false;
    private String os = System.getProperty("os.name").toLowerCase();

    public WebSocketEndPoint(){
        this.port = 9222;
        defaultPath();
    }

    public WebSocketEndPoint(int port){
        this.port = port;
        defaultPath();
    }

    public WebSocketEndPoint(String browserPath){
        this.port = 9222;
        this.browserPath = browserPath;
        haveBrowserPath = new File(this.browserPath).exists();
    }

    public WebSocketEndPoint(int port, String browserPath){
        this.port = port;
        this.browserPath = browserPath;
        haveBrowserPath = new File(this.browserPath).exists();
    }

    private void defaultPath(){
        String macPath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
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

    public int getPort(){ return this.port; }

    public boolean browserPathExist(){ return this.haveBrowserPath; }

    public void startWebSocket() throws RuntimeException{
        File tempDir;

        if(haveBrowserPath){
            ProcessBuilder pb;
            if(os.contains("mac")){
                tempDir = new File("/tmp/chrome_debug");
                if(!tempDir.exists()){
                    System.out.println("tempDir doesn't exist.");
                    try{
                        Process warmupProcess = new ProcessBuilder(this.browserPath,
                                "--remote-debugging-port="+this.port,
                                "--user-data-dir=/tmp/chrome_debug").start();
                        Thread.sleep(2500);

                        warmupProcess.destroy();
                        Thread.sleep(500);
                    }catch(IOException | InterruptedException e){
                        System.out.println("solution failed.");
                    }
                }

                pb = new ProcessBuilder(this.browserPath,
                        "--remote-debugging-port="+this.port,
                        "--user-data-dir=/tmp/chrome_debug",
                        "--no-first-run",
                        "--no-default-browser-check");
                try{
                    pb.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else if(os.contains("win")){
                pb = new ProcessBuilder(this.browserPath,
                        "--remote-debugging-port="+this.port,
                        "--user-data-dir=C:\\ChromeProfile",
                        "--no-first-run",
                        "--no-default-browser-check");
                try{
                    pb.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}