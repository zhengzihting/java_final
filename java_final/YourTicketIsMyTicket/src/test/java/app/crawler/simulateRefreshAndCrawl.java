package app.crawler;

import app.ticketData.TicketList;

import java.util.Scanner;

public class simulateRefreshAndCrawl {
    public static void main(String[] args){
        String url = "https://kktix.com/events";
        KktixCrawler kktixCrawler = new KktixCrawler(url);
        TicketList ticketList;
        Scanner scanner = new Scanner(System.in);

        kktixCrawler.startCrawler();
        System.out.print("crawl? (y): ");
        if(scanner.nextLine().equals("y")){
            for(int t = 1; t <= 5; t++){
                System.out.println("Round " + t + " :");
                ticketList = kktixCrawler.crawlTickets();
                System.out.println(ticketList);
                kktixCrawler.refreshPage();
            }
        }
        kktixCrawler.closeCrawler();
    }
}