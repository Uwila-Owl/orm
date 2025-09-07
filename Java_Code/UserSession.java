/**
 * Classe Singleton pour gérer la session utilisateur
 * Fichier: UserSession.java
 */
public class UserSession {
    private static UserSession instance;
    private String userId;
    private String nom;
    private String prenom;
    private String email;
    private boolean isSuperviseur;
    private long loginTime;
    
    private UserSession() {}
    
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    
    // Setters
    public void setUserInfo(String userId, String nom, String prenom, String email, boolean isSuperviseur) {
        this.userId = userId;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.isSuperviseur = isSuperviseur;
        this.loginTime = System.currentTimeMillis();
    }
    
    // Getters
    public String getUserId() { 
        return userId; 
    }
    
    public String getNom() { 
        return nom; 
    }
    
    public String getPrenom() { 
        return prenom; 
    }
    
    public String getEmail() { 
        return email; 
    }
    
    public boolean isSuperviseur() { 
        return isSuperviseur; 
    }
    
    public String getFullName() {
        if (prenom != null && nom != null) {
            return prenom + " " + nom;
        } else if (nom != null) {
            return nom;
        } else if (userId != null) {
            return userId;
        }
        return "Utilisateur";
    }
    
    // Vérifier si la session est expirée (8 heures)
    public boolean isSessionExpired() {
        if (loginTime == 0) return true;
        long sessionDuration = System.currentTimeMillis() - loginTime;
        return sessionDuration > (8 * 60 * 60 * 1000);
    }
    
    // Obtenir le temps de session restant en minutes
    public long getRemainingSessionMinutes() {
        if (loginTime == 0) return 0;
        long elapsed = System.currentTimeMillis() - loginTime;
        long maxSession = 8 * 60 * 60 * 1000;
        return Math.max(0, (maxSession - elapsed) / (60 * 1000));
    }
    
    public void clear() {
        userId = null;
        nom = null;
        prenom = null;
        email = null;
        isSuperviseur = false;
        loginTime = 0;
    }
}
