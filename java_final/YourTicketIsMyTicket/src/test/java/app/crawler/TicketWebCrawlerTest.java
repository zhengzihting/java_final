package app.crawler;

public class TicketWebCrawlerTest {
    public static void main(String[] args){
        System.out.println("create ticketWebCrawler...");
        TicketWebCrawler ticketWebCrawler = new TicketWebCrawler("https://ticket.ibon.com.tw/ActivityInfo/Details/39484");
        System.out.println("test ticketWebCrawler's function...");
        ticketWebCrawler.startCrawler();
    }
}