package tn.esprit.skylora.entities;

import java.time.LocalDateTime;

public class MessageTicket {

    private int id;
    private int ticketId;
    private int utilisateurId;
    private String contenu;
    private LocalDateTime dateEnvoi;
    private boolean isInternal;
    private String attachmentsJson;

    public MessageTicket() {
    }

    public MessageTicket(int ticketId, int utilisateurId, String contenu) {
        this.ticketId = ticketId;
        this.utilisateurId = utilisateurId;
        this.contenu = contenu;
    }

    public MessageTicket(int ticketId, int utilisateurId, String contenu, boolean isInternal, String attachmentsJson) {
        this.ticketId = ticketId;
        this.utilisateurId = utilisateurId;
        this.contenu = contenu;
        this.isInternal = isInternal;
        this.attachmentsJson = attachmentsJson;
    }

    // getters setters
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

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public boolean isInternal() {
        return isInternal;
    }

    public void setInternal(boolean internal) {
        isInternal = internal;
    }

    public String getAttachmentsJson() {
        return attachmentsJson;
    }

    public void setAttachmentsJson(String attachmentsJson) {
        this.attachmentsJson = attachmentsJson;
    }
}
