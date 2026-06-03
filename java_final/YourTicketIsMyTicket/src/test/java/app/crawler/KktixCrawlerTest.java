package app.crawler;

import app.ticketData.TicketList;

public class KktixCrawlerTest {
    public static void main(String[] args) throws InterruptedException {
        String url = "https://kktix.com/events/5cf8cfb9/registrations/new";
        KktixCrawler kktixCrawler = new KktixCrawler(url);
        TicketList ticketList;

        kktixCrawler.startCrawler();
        ticketList = kktixCrawler.crawlTickets();
        System.out.println(ticketList);
        kktixCrawler.closeCrawler();
    }
}