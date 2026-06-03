package app.crawler;

public class TicketWebCrawlerTest {
    public static void main(String[] args){
        System.out.println("Create ticketWebCrawler...");
        TicketWebCrawler ticketWebCrawler = new TicketWebCrawler("https://ticket.ibon.com.tw/ActivityInfo/Details/39484");
        System.out.println("Test ticketWebCrawler's function...");
        ticketWebCrawler.startCrawler();
        System.out.println("Start crawler successfully!");
    try{
        Thread.sleep(3000);
        ticketWebCrawler.closeCrawler();
        System.out.println("Stop crawler successfully!");
    }catch(InterruptedException e) {
        System.err.println("InterruptedException: " + e);
    }
    }
}