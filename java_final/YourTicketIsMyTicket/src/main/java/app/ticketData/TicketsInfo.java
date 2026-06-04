package app.ticketData;

public class TicketsInfo {
    public enum StatusType {SELLING, SOLD_OUT, CLOSED};

    private String ticketType;
    private int ticketPrice;
    private StatusType ticketStatus;

    public TicketsInfo(String ticketType, int ticketPrice, StatusType ticketStatus){
        this.ticketType = ticketType;
        this.ticketPrice = ticketPrice;
        this.ticketStatus = ticketStatus;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public void setTicketPrice(int ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public void setTicketStatus(StatusType ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    @Override
    public String toString(){
        return String.format("ticketType: %s\nticketPrice: %d\nticketStatus: "+ticketStatus+"\n\n",
                ticketType, ticketPrice);
    }
}