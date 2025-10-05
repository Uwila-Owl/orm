package com.uml.generator.export;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DiagramSave {
    private List<Map<String, Object>> entites;
    private List<Map<String, Object>> relations;
    private int schemaId;
    private boolean isUML;
    private Date dateSauvegarde;
    private String version = "1.0";
    
    public DiagramSave() {
        this.dateSauvegarde = new Date();
    }
    
    // Getters et setters
    public List<Map<String, Object>> getEntites() { return entites; }
    public void setEntites(List<Map<String, Object>> entites) { this.entites = entites; }
    
    public List<Map<String, Object>> getRelations() { return relations; }
    public void setRelations(List<Map<String, Object>> relations) { this.relations = relations; }
    
    public int getSchemaId() { return schemaId; }
    public void setSchemaId(int schemaId) { this.schemaId = schemaId; }
    
    public boolean isUML() { return isUML; }
    public void setUML(boolean UML) { isUML = UML; }
    
    public Date getDateSauvegarde() { return dateSauvegarde; }
    public void setDateSauvegarde(Date dateSauvegarde) { this.dateSauvegarde = dateSauvegarde; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}
