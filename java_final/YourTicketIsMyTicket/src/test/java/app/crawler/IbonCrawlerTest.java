package app.crawler;

// only for demo, not used
public class IbonCrawlerTest {
    public static void main(String[] args) throws InterruptedException {
        String url = "https://ticket.ibon.com.tw/ActivityInfo/Details/39611";
        IbonCrawler ibonCrawler = new IbonCrawler(url);

        ibonCrawler.startCrawler();
        try{
            Thread.sleep(2000);
            if(ibonCrawler.signedIn()){
                System.out.println("已登入");
                if(ibonCrawler.haveTicket()) System.out.println("有票");
                else System.out.println("沒票");
            }else System.out.println("未登入");
        }catch(InterruptedException e){
            throw e;
        }catch(NullPointerException e){
            System.err.println("NullPointerException: "+e);
        }
    }
}