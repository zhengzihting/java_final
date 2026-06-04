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

    public String getTicketType(){ return this.ticketType; }

    public int getTicketPrice(){ return this.ticketPrice; }

    public StatusType getTicketStatus(){ return this.ticketStatus; }

    @Override
    public String toString(){
        return String.format("ticketType: %s\nticketPrice: %d\nticketStatus: "+ticketStatus+"\n\n",
                ticketType, ticketPrice);
    }
}