package fr.openent.lystore.model;

import io.vertx.core.json.JsonObject;

public class AccountingProgram extends  Model {

    String programSection;
    String programChapter;
    String functionnalCode;
    String name;
    String section;
    String chapter;


    String label;
    @Override
    public JsonObject toJsonObject() {
        return null;
    }

    public String getProgramSection() {
        return programSection;
    }

    public void setProgramSection(String programSection) {
        this.programSection = programSection;
    }

    public String getProgramChapter() {
        return programChapter;
    }

    public void setProgramChapter(String programChapter) {
        this.programChapter = programChapter;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public String getFunctionnalCode() {
        return functionnalCode;
    }

    public void setFunctionnalCode(String functionnalCode) {
        this.functionnalCode = functionnalCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getLabel() { return label; }

    public void setLabel(String label) { this.label = label; }
}
