package app.crawler;

import com.microsoft.playwright.*;

public abstract class TicketWebCrawler {
    private String url;
    private final String localHostUrl = "http://localhost:";

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
            ws = new WebSocketEndPoint();
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
            ws = new WebSocketEndPoint(port);
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

    public void closeCrawler(){
        buyTicketPage.close();
        playwright.close();
    }
}