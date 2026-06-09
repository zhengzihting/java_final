package app.crawler;

import app.crawler.websocket.MacWebSocket;
import app.crawler.websocket.WebSocketEndPoint;
import app.crawler.websocket.WinWebSocket;
import com.microsoft.playwright.*;

public abstract class TicketWebCrawler {
    private String url;
    private final String localHostUrl = "http://localhost:";

    private String os = System.getProperty("os.name").toLowerCase();
    private Playwright playwright;
    private WebSocketEndPoint ws;
    private Browser browser;
    private BrowserContext defaultContext;
    private Page buyTicketPage;

    public TicketWebCrawler(String url){
        this.url = url;
    }

    public Page getBuyTicketPage(){ return this.buyTicketPage; }

    public void startCrawler(){
        try{
            playwright = Playwright.create();
            if(os.contains("mac")) ws = new MacWebSocket();
            else if(os.contains("win")) ws = new WinWebSocket();
            else{
                System.err.println("No support this os.");
            }
            ws.startWebSocket();

            // 建立CDP的browser創建
            browser = playwright.chromium().connectOverCDP(String.format("%s%d", localHostUrl, ws.getPort()));
            // 尋找建立CDP browser後預設開好的BrowserContext
            defaultContext = browser.contexts().get(0);
            // 找預設開好的Page
            buyTicketPage = defaultContext.pages().get(0);
            buyTicketPage.navigate(url);
        }catch(RuntimeException e){
            System.err.println("RuntimeException: "+e);
        }
    }

    public void startCrawler(int port){
        try{
            playwright = Playwright.create();
            if(os.contains("mac")) ws = new MacWebSocket(port);
            else if(os.contains("win")) ws = new WinWebSocket(port);
            ws.startWebSocket();

            // 建立CDP的browser創建
            browser = playwright.chromium().connectOverCDP(String.format("%s%d", localHostUrl, ws.getPort()));
            // 尋找建立CDP browser後預設開好的BrowserContext
            defaultContext = browser.contexts().get(0);
            // 找預設開好的Page
            buyTicketPage = defaultContext.pages().get(0);
            buyTicketPage.navigate(url);
        }catch(RuntimeException e){
            System.err.println("RuntimeException: "+e);
        }
    }

    public void refreshPage(){
        buyTicketPage.reload();
    }

    public void closeCrawler(){
        buyTicketPage.close();
        playwright.close();
    }
}