package com.linmalu.webcontroller.server;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.linmalu.webcontroller.Main;

public class LinmaluSecurity
{
	private static final IvParameterSpec iv = new IvParameterSpec(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
	private final OfflinePlayer player;
	private final SecretKeySpec key;

	@SuppressWarnings("deprecation")
	public LinmaluSecurity(String data) throws Exception
	{
		JsonObject json = (JsonObject)new JsonParser().parse(data);
		player = Bukkit.getOfflinePlayer(json.get("name").getAsString());
		this.key = new SecretKeySpec(Main.getMain().getPlayersConfig().getData(player.getUniqueId().toString(), String.class).getBytes(Charset.forName("UTF-8")), "AES");
		if(player.isBanned() || !decrypt(json.get("data").getAsString()).equals("LinmaluWebController"))
		{
			throw new Exception();
		}
	}
	public OfflinePlayer getPlayer()
	{
		return player;
	}
	public String encrypt(String text) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		return Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes("UTF-8")));
	}
	public String decrypt(String text) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key, iv);
		return new String(cipher.doFinal(Base64.getDecoder().decode(text)), Charset.forName("UTF-8"));
	}

	public static String createKey(String pwd) throws Exception
	{
		return toHex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(new PBEKeySpec(pwd.toCharArray(), toHex(MessageDigest.getInstance("SHA-256").digest(pwd.getBytes(Charset.forName("UTF-8")))).getBytes(Charset.forName("UTF-8")), 1024, 16 * 4)).getEncoded());
	}
	private static String toHex(byte[] data)
	{
		StringBuilder sb = new StringBuilder();
		for(byte b : data)
		{
			sb.append(Integer.toString((b & 0xFF) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}
}
