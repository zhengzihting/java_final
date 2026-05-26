package app.crawler;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class IbonCrawler extends TicketWebCrawler {

    public IbonCrawler(String url){
        super(url);
    }

    public boolean isSignIn(){
        Locator locator = getBuyTicketPage().getByText("快速登入", new Page.GetByTextOptions().setExact(true));
        if(locator != null) return true;
        else return false;
    }
}