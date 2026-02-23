package tn.esprit.skylora.entities;

import java.time.LocalDateTime;

public class Feedback {
    private int id;
    private int ticketId;
    private int rating;
    private String comment;
    private LocalDateTime dateCreation;

    public Feedback() {
    }

    public Feedback(int ticketId, int rating, String comment) {
        this.ticketId = ticketId;
        this.rating = rating;
        this.comment = comment;
        this.dateCreation = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "id=" + id +
                ", ticketId=" + ticketId +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                ", dateCreation=" + dateCreation +
                '}';
    }
}
