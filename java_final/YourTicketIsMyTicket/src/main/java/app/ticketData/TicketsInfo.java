package app.ticketData;

public class TicketsInfo {
    public enum StatusType {SELLING, SOLD_OUT, CLOSED};

    private String ticketType;
    private String ticketPrice;
    private StatusType ticketStatus;

    public TicketsInfo(String ticketType, String ticketPrice, StatusType ticketStatus){
        this.ticketType = ticketType;
        this.ticketPrice = ticketPrice;
        this.ticketStatus = ticketStatus;
    }

    public String getTicketType(){ return this.ticketType; }

    public String getTicketPrice(){ return this.ticketPrice; }

    public StatusType getTicketStatus(){ return this.ticketStatus; }

    @Override
    public String toString(){
        return String.format("ticketType: %s\nticketPrice: %d\nticketStatus: "+ticketStatus+"\n\n",
                ticketType, ticketPrice);
    }
}