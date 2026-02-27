package com.skilora.finance.model;

import java.time.LocalDateTime;

/**
 * Paiement (Stripe - mode TEST)
 * Modèle métier pour enregistrer les paiements d'un projet.
 */
public class Paiement {

    public enum Statut {
        PENDING,
        SUCCESS,
        FAILED
    }

    private int id;
    private double montant;
    private LocalDateTime dateHeure;
    private Statut statut;
    private String stripePaymentId;
    private String referenceProjet;
    private String nomBeneficiaire;

    public Paiement() {
    }

    public Paiement(int id,
                    double montant,
                    LocalDateTime dateHeure,
                    Statut statut,
                    String stripePaymentId,
                    String referenceProjet,
                    String nomBeneficiaire) {
        this.id = id;
        this.montant = montant;
        this.dateHeure = dateHeure;
        this.statut = statut;
        this.stripePaymentId = stripePaymentId;
        this.referenceProjet = referenceProjet;
        this.nomBeneficiaire = nomBeneficiaire;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    public void setDateHeure(LocalDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public String getStripePaymentId() {
        return stripePaymentId;
    }

    public void setStripePaymentId(String stripePaymentId) {
        this.stripePaymentId = stripePaymentId;
    }

    public String getReferenceProjet() {
        return referenceProjet;
    }

    public void setReferenceProjet(String referenceProjet) {
        this.referenceProjet = referenceProjet;
    }

    public String getNomBeneficiaire() {
        return nomBeneficiaire;
    }

    public void setNomBeneficiaire(String nomBeneficiaire) {
        this.nomBeneficiaire = nomBeneficiaire;
    }

    @Override
    public String toString() {
        return "Paiement{" +
                "id=" + id +
                ", montant=" + montant +
                ", dateHeure=" + dateHeure +
                ", statut=" + statut +
                ", stripePaymentId='" + stripePaymentId + '\'' +
                ", referenceProjet='" + referenceProjet + '\'' +
                ", nomBeneficiaire='" + nomBeneficiaire + '\'' +
                '}';
    }
}

