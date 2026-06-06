package app.ticketData;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TicketList {
    private List<TicketsInfo> ticketsInfo = new ArrayList<>();

    public void setTickets(List<Locator> ticketUnits) throws NullPointerException{
        String ticketType, ticketPrice;
        Locator statusContent;
        TicketsInfo.StatusType ticketStatus;

        for(Locator ticketUnit: ticketUnits){
            ticketType = ticketUnit.locator("span.ticket-name").innerText();
            ticketPrice = ticketUnit.locator("span.ticket-price").innerText();
            statusContent = ticketUnit.locator("span.ticket-quantity");
            if(statusContent.getByRole(AriaRole.BUTTON).first().isVisible() || statusContent.getByText("需要資格").isVisible()){
                ticketStatus = TicketsInfo.StatusType.SELLING;
            }else if(statusContent.getByText("已售完").isVisible()){
                ticketStatus = TicketsInfo.StatusType.SOLD_OUT;
            }else if(statusContent.getByText("結束販售").isVisible() || statusContent.getByText("尚未開賣").isVisible()){
                ticketStatus = TicketsInfo.StatusType.CLOSED;
            }else throw new NullPointerException();

            ticketsInfo.add(new TicketsInfo(ticketType, ticketPrice, ticketStatus));
        }
    }

    public boolean haveTicket(){
        if(ticketsInfo.isEmpty()) return false;
        else return true;
    }

    public List<TicketsInfo> getTicketsInfo(){
        return ticketsInfo;
    }

    @Override
    public String toString(){
        String str = "";
        for(TicketsInfo ticket: ticketsInfo){
            str += ticket;
        }
        return str;
    }
}