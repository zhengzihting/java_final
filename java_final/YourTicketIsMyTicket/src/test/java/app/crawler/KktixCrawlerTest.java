package app.crawler;

// only for demo, not used
public class KktixCrawlerTest {
    public static void main(String[] main) throws InterruptedException {
        String url = "https://kktix.com/events/alicefly0605/registrations/new";
        KktixCrawler kktixCrawler = new KktixCrawler(url);

        kktixCrawler.startCrawler();
        try{
            Thread.sleep(2000);
            if(kktixCrawler.haveTicket()) System.out.println("有票");
            else System.out.println("沒票");
        }catch(InterruptedException e){
            throw e;
        }catch(NullPointerException e){
            System.err.println("NullPointerException: "+e);
        }
    }
}