package tn.esprit.skylora.entities;

import java.time.LocalDateTime;

public class Ticket {

    private int id;
    private int utilisateurId;
    private String subject;
    private String categorie;
    private String priorite;
    private String statut;
    private String description;
    private LocalDateTime dateCreation;
    private LocalDateTime dateResolution;
    private Integer agentId;

    public Ticket() {
    }

    public Ticket(int utilisateurId, String subject, String categorie, String priorite, String statut,
            String description) {
        this.utilisateurId = utilisateurId;
        this.subject = subject;
        this.categorie = categorie;
        this.priorite = priorite;
        this.statut = statut;
        this.description = description;
    }

    public Ticket(int id, int utilisateurId, String subject, String categorie, String priorite,
            String statut, String description, LocalDateTime dateCreation, LocalDateTime dateResolution,
            Integer agentId) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.subject = subject;
        this.categorie = categorie;
        this.priorite = priorite;
        this.statut = statut;
        this.description = description;
        this.dateCreation = dateCreation;
        this.dateResolution = dateResolution;
        this.agentId = agentId;
    }

    // getters setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getPriorite() {
        return priorite;
    }

    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateResolution() {
        return dateResolution;
    }

    public void setDateResolution(LocalDateTime dateResolution) {
        this.dateResolution = dateResolution;
    }

    public Integer getAgentId() {
        return agentId;
    }

    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }
}
