package com.radproject.votingapp.model;

public class Candidate {

    String name;
    String party;
    String election;
    String image;
    String id;

    public Candidate(String name, String party, String election, String image, String id) {
        this.name = name;
        this.party = party;
        this.election = election;
        this.image = image;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParty() {
        return party;
    }

    public void setParty(String party) {
        this.party = party;
    }

    public String getElection() {
        return election;
    }

    public void setElection(String election) {
        this.election = election;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
