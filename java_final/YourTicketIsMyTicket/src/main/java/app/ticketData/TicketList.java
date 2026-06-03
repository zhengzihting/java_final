package app.ticketData;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TicketList {
    private final int MAXSIZE = 1000;
    private TicketsInfo[] ticketsInfo = new TicketsInfo[MAXSIZE];
    private final Pattern pricePattern = Pattern.compile("\\d+");
    private int ticketsCount;

    public void setTickets(List<Locator> ticketUnits) throws NullPointerException{
        Matcher m;
        String ticketType, ticketPriceStr;
        int ticketPrice;
        Locator ticketStatus;

        ticketsCount = 0;
        for(Locator ticketUnit: ticketUnits){
            ticketsInfo[ticketsCount] = new TicketsInfo();

            ticketType = ticketUnit.locator("span.ticket-name").innerText();
            ticketsInfo[ticketsCount].setTicketType(ticketType);

            ticketPriceStr = ticketUnit.locator("span.ticket-price").innerText();
            m = pricePattern.matcher(ticketPriceStr);
            while(m.find()){
               ticketPriceStr = new StringBuilder().append(m.group()).toString();
            }
            ticketPrice = Integer.parseInt(ticketPriceStr);
            ticketsInfo[ticketsCount].setTicketPrice(ticketPrice);

            ticketStatus = ticketUnit.locator("span.ticket-quantity");
            if(ticketStatus.getByRole(AriaRole.BUTTON).first().isVisible()){
                ticketsInfo[ticketsCount].setTicketStatus(TicketsInfo.StatusType.SELLING);
            }else if(ticketStatus.getByText("已售完").isVisible()){
                ticketsInfo[ticketsCount].setTicketStatus(TicketsInfo.StatusType.SOLD_OUT);
            }else if(ticketStatus.getByText("結束販售").isVisible()){
                ticketsInfo[ticketsCount].setTicketStatus(TicketsInfo.StatusType.CLOSED);
            }else throw new NullPointerException();

            ticketsCount++;
        }
    }

    @Override
    public String toString(){
        String str = "";
        for(int c = 0; c < ticketsCount; c++){
            str += ticketsInfo[c].toString();
        }
        return str;
    }
}