package fr.openent.lystore.model;

import io.vertx.core.json.JsonObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Model Class for projects
 */
public class Project extends Model{

    private String regroupement ;
    private String name;
    private String comment;
    private List<String> structure_groups = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private String room;
    private String building;
    private int rank;
    private boolean hasRank = false;

    @Override
    public JsonObject toJsonObject() {
        return null;
    }

    public String getRegroupement() {
        return regroupement;
    }

    public void setRegroupement(String regroupement) {
        this.regroupement = regroupement;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.hasRank = true;
        this.rank = rank;
    }

    public boolean hasRank() {
        return hasRank;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void addTag(String tag){
        this.tags.add(tag);
    }

    public void addStructureGroup(String structureGroup){
        this.structure_groups.add(structureGroup);
    }
    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getStructureGroupString() {
        return this.structure_groups.toString();
    }

    public List<String> getTags() {
        return tags;
    }
}
