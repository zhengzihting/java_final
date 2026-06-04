package app.crawler;

import app.ticketData.TicketList;
import java.util.Scanner;

public class KktixCrawlerTest {
    public static void main(String[] args){
        String url = "https://comedyclub.kktix.cc/events/alicefly", input;
        KktixCrawler kktixCrawler = new KktixCrawler(url);
        TicketList ticketList;
        Scanner scanner = new Scanner(System.in);

        kktixCrawler.startCrawler();
        System.out.print("crawl? (y/n): ");
        input = scanner.nextLine();
        while(input.equals("y")){
            ticketList = kktixCrawler.crawlTickets();
            System.out.println(ticketList);
            System.out.print("crawl? (y/n): ");
            input = scanner.nextLine();
            if(input.equals("n")) break;
            else{
                while(!input.equals("y") && !input.equals("n")){
                    System.out.print("Wrong instruction.");
                    System.out.print("crawl? (y/n): ");
                    input = scanner.nextLine();
                }
            }
        }
        kktixCrawler.closeCrawler();
    }
}