package app.crawler;

import com.microsoft.playwright.*;

public class TicketWebCrawler {
    private String url;

    public TicketWebCrawler(String url){
        this.url = url;
    }

    public void startCrawler(){
        try(Playwright playwright = Playwright.create()){
            try{
            }catch(Exception e){
                System.err.printf(e.toString());
            }
        }
    }
}