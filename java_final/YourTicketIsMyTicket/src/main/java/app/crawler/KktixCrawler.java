package app.crawler;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;

public class KktixCrawler extends TicketWebCrawler {

    public KktixCrawler(String url){
        super(url);
    }

    public boolean haveTicket() throws NullPointerException{
        Locator sellTicketButton = getBuyTicketPage().getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setDisabled(false)).first();
        Locator soldOutTicketText = getBuyTicketPage().getByText(Pattern.compile(" 已售完 ", Pattern.CASE_INSENSITIVE)).first();
        if(sellTicketButton.isVisible()){
            return true;
        }else if(soldOutTicketText.isVisible()){
            return false;
        }else{
            throw new NullPointerException("兩個按鈕都找不到！\n");
        }
    }
}