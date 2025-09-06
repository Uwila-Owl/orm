import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoUtils {
    private static final String ALGO = "AES";
    private static final String APP_KEY = "9582047136598302"; // Clé de 16 caractères

    private static byte[] getKey() {
        return APP_KEY.getBytes();
    }

    public static String decrypt(String encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGO);
        SecretKeySpec secretKey = new SecretKeySpec(getKey(), ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        return new String(cipher.doFinal(decoded));
    }

    // Méthode optionnelle pour chiffrer des données (utile pour générer de nouvelles valeurs chiffrées)
    public static String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGO);
        SecretKeySpec secretKey = new SecretKeySpec(getKey(), ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // Méthode pour vérifier le déchiffrement
    public static void main(String[] args) {
        try {
            System.out.println("Déchiffrement :");
            System.out.println("URL déchiffrée : " + decrypt("GNRil/C6uUrblMGNoAwln3ou/Qur5D0F7TnTRwu0xFTBpgHwHkYi3860jf6xlb751Fx4biJTPzVgYHvmoT9SZA=="));
            System.out.println("User déchiffré : " + decrypt("DqehAQH6lFaBqwZ+XHfSCw=="));
            System.out.println("Password déchiffré : " + decrypt("L8iccJzrr5ZQiJ2q8LKFGg=="));
        } catch (Exception e) {
            System.err.println("Erreur de déchiffrement : " + e.getMessage());
            e.printStackTrace();
        }
    }
}