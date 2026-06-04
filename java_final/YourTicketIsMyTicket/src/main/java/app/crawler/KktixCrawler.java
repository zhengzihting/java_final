package app.crawler;

import app.ticketData.TicketList;
import com.microsoft.playwright.Locator;
import java.util.List;

public class KktixCrawler extends TicketWebCrawler {

    public KktixCrawler(String url){
        super(url);
    }

    public TicketList crawlTickets() {
        TicketList ticketList = new TicketList();
        try{
            List<Locator> ticketUnits = getBuyTicketPage().locator("div.ticket-unit").all();
            ticketUnits.get(0).waitFor(new Locator.WaitForOptions().setTimeout(30000));
            Thread.sleep(5000);

            ticketList.setTickets(ticketUnits);
        }catch(InterruptedException e){
            System.err.println("crawlTickets InterruptedException:");
            System.err.println(e);
        }catch(NullPointerException e){
            System.err.println("tickets only get:");
            System.err.println(ticketList);
            System.err.println("crawlTickets NullPointerException:");
            System.err.println(e);
            throw e;
        }catch(RuntimeException e) {
            System.err.println("crawlTickets RuntimeException:");
            System.err.println(e);
        }

        return ticketList;
    }
}