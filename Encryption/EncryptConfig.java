import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class EncryptConfig {
    private static final String ALGO = "AES";

    private static byte[] getKey() {
        String key = System.getenv("APP_KEY");
        if (key == null || key.length() != 16) {
            throw new RuntimeException("❌ Variable d'environnement APP_KEY manquante ou invalide (16 chars)");
        }
        return key.getBytes();
    }

    public static String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGO);
        SecretKeySpec secretKey = new SecretKeySpec(getKey(), ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://141.253.96.168:5432/GenerateurUML";
        String user = "postgres";
        String password = "i3d_0rm2025";

        System.out.println("db.url=" + encrypt(url));
        System.out.println("db.user=" + encrypt(user));
        System.out.println("db.password=" + encrypt(password));
    }
}

