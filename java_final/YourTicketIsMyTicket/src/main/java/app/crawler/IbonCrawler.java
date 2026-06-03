package app.crawler;

import com.microsoft.playwright.*;
import java.util.regex.Pattern;

public class IbonCrawler extends TicketWebCrawler {

    public IbonCrawler(String url){
        super(url);
    }

    public boolean signedIn() throws NullPointerException{
        Locator signInText = getBuyTicketPage().getByText("快速登入",
                new Page.GetByTextOptions().setExact(true));
        Locator signOutText = getBuyTicketPage().getByText("登出",
                new Page.GetByTextOptions().setExact(true));
        if(signInText.isVisible()){
            return false;
        }else if(signOutText.isVisible()){
            return true;
        }else{
            throw new NullPointerException("網路不穩，請稍後再試！\n");
        }
    }

    public boolean haveTicket() throws NullPointerException{
        Locator sellTicketText = getBuyTicketPage().getByText(Pattern.compile("^線上購票$", Pattern.CASE_INSENSITIVE));
        Locator soldOutTicketText = getBuyTicketPage().getByText(Pattern.compile("^已售完$", Pattern.CASE_INSENSITIVE));
        if(sellTicketText.isVisible()){
            return true;
        }else if(soldOutTicketText.isVisible()){
            return false;
        }else{
            throw new NullPointerException("兩個按鈕都找不到！\n");
        }
    }
}