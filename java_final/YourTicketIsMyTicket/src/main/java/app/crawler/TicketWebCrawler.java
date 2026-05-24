package app.crawler;

import com.microsoft.playwright.*;

public class TicketWebCrawler {
    private String url;
    private Browser browser;

    public TicketWebCrawler(String url){
        this.url = url;
    }

    public void startCrawler(){
        try(Playwright playwright = Playwright.create()){
            browser = playwright.chromium().connectOverCDP(url);
        }
    }
}