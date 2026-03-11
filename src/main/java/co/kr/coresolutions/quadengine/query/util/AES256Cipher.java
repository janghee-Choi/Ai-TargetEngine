package co.kr.coresolutions.quadengine.query.util;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;

public class AES256Cipher {

	private final static String DEFAULT_SECRET_KEY = "SecRetKey:EasyCore-Solutions-Inc";
	private static String TRIGGER_FILE_PATH = "/data/encrypt/aes256.key";
	private static String SECRET_KEY = initializeSecretKey();

	private static String initializeSecretKey() {
		try {
			File triggerFile = new File(TRIGGER_FILE_PATH);
			if (triggerFile.exists()) {
				String keyFromFile = new String(Files.readAllBytes(triggerFile.toPath()), StandardCharsets.UTF_8).trim();
				if (!keyFromFile.isEmpty()) {
					return keyFromFile;
				}
			}
			return DEFAULT_SECRET_KEY;
		} catch (Exception e) {
			return DEFAULT_SECRET_KEY;
		}
	}

	public static void setSecretKey(String secretKey) {
		SECRET_KEY = secretKey;
	}

	public static void setTriggerFilePath(String triggerFilePath) {
		TRIGGER_FILE_PATH = triggerFilePath;
		SECRET_KEY = initializeSecretKey();
	}

	public static String AES_Encode(String str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		return AES_Encode(str, true);
	}

	public static String AES_Decode(String str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		return AES_Decode(str, true);
	}

	//암호화
	public static String AES_Encode(String str, boolean bEncrypt) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		if (!bEncrypt || str == null) {
			return str;
		}

		byte[] keyData = SECRET_KEY.getBytes();
		String IV = SECRET_KEY.substring(0, 16);

		SecretKey secureKey = new SecretKeySpec(keyData, "AES");

		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		c.init(Cipher.ENCRYPT_MODE, secureKey, new IvParameterSpec(IV.getBytes()));

		byte[] encrypted = c.doFinal(str.getBytes(StandardCharsets.UTF_8));

		return new String(java.util.Base64.getEncoder().encode(encrypted));
	}

	//복호화
	public static String AES_Decode(String str, boolean bEncrypt) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		if (!bEncrypt || str == null) {
			return str;
		}

		byte[] keyData = SECRET_KEY.getBytes();
		String IV = SECRET_KEY.substring(0, 16);

		SecretKey secureKey = new SecretKeySpec(keyData, "AES");

		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		c.init(Cipher.DECRYPT_MODE, secureKey, new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8)));

		byte[] byteStr = java.util.Base64.getDecoder().decode(str);

		return new String(c.doFinal(byteStr), StandardCharsets.UTF_8);
	}

	public static String SHA_Encode(String str) throws NoSuchAlgorithmException {
		return SHA_Encode(str, true);
	}

	public static String SHA_Encode(String str, boolean bEncrypt) throws NoSuchAlgorithmException {
		if (!bEncrypt || str == null) {
			return str;
		}

		MessageDigest md = MessageDigest.getInstance("SHA-512");
		byte[] salt = generateSalt();
		md.update(salt);
		byte[] msgb = md.digest(str.getBytes());

		StringBuilder sb = new StringBuilder();
		sb.append(bytesToHex(salt));
		sb.append(bytesToHex(msgb));
		return sb.toString();
	}

	public static Boolean SHA_Compare(String str, String encStr) throws NoSuchAlgorithmException {
		byte[] salt = hexToBytes(encStr.substring(0, 32));
		String orgStr = encStr.substring(32);

		MessageDigest md = MessageDigest.getInstance("SHA-512");
		md.update(salt);
		byte[] msgb = md.digest(str.getBytes());
		String compStr = bytesToHex(msgb);

		return compStr.equals(orgStr);
	}

	private static byte[] generateSalt() {
		SecureRandom sr = new SecureRandom();
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		return salt;
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private static byte[] hexToBytes(String hex) {
		int len = hex.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
					+ Character.digit(hex.charAt(i + 1), 16));
		}
		return data;
	}
}
