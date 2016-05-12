package com.linmalu.webcontroller.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.linmalu.library.api.LinmaluUtility;
import com.linmalu.library.api.LinmaluYamlConfiguration;
import com.linmalu.webcontroller.Main;

public class LinmaluWebClient implements Runnable
{
	private final Socket client;

	public LinmaluWebClient(Socket client)
	{
		this.client = client;
		new Thread(this).start();
	}
	@Override
	public void run()
	{
		try
		{
			byte[] data = new byte[1024];
			int size = client.getInputStream().read(data);
			String[] headers = new String(data, 0, size).replace("\\r\\n\\r\\n", "").split("\\r\\n");
			DataOutputStream dos = new DataOutputStream(client.getOutputStream());
			switch(headers[0])
			{
				case "GET / HTTP/1.1":
					InputStream in = getClass().getResourceAsStream("/com/linmalu/webcontroller/html/LinmaluWebController.html");
					data = new byte[in.available()];
					size = in.read(data);
					dos.writeBytes("HTTP/1.1 200 OK \r\n");
					dos.writeBytes("Content-Type: text/html; charset=UTF-8\r\n");
					dos.writeBytes("Content-Length: " + size + "\r\n");
					dos.writeBytes("\r\n");
					dos.write(data, 0, size);
					dos.writeBytes("\r\n");
					dos.flush();
					break;
				case "GET /LinmaluWebController HTTP/1.1":
					String key = null;
					for(String header : headers)
					{
						if(header.startsWith("Sec-WebSocket-Key: "))
						{
							key = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((header.replace("Sec-WebSocket-Key: ", "") + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes()));
						}
					}
					if(key != null)
					{
						dos.writeBytes("HTTP/1.1 101 Web Socket Protocol Handshake\r\n");
						dos.writeBytes("Upgrade: WebSocket\r\n");
						dos.writeBytes("Connection: Upgrade\r\n");
						dos.writeBytes("Sec-WebSocket-Accept: " + key + "\r\n");
						dos.writeBytes("\r\n");
						dos.flush();
						int count = 0;
						while(true)
						{
							if(count == 10)
							{
								break;
							}
							else
							{
								count++;
							}
							size = client.getInputStream().read(data);
							String msg = getWebSocketMessage(data, size);
							if(msg != null && msg.startsWith("Password : "))
							{
								if(msg.equals("Password : " + LinmaluYamlConfiguration.loadConfiguration(new File(Main.getMain().getDataFolder(), "config.yml")).getString("Password")))
								{
									try
									{
										dos.write(setWebSocketMessage(LinmaluUtility.consoleToText(new String(Files.readAllBytes(Paths.get("./logs/latest.log")), Charset.defaultCharset()))));
										dos.flush();
										Bukkit.getConsoleSender().sendMessage(Main.getMain().getTitle() + ChatColor.GOLD + client.getInetAddress().getHostAddress() + ":" + client.getPort() + ChatColor.YELLOW + " <-> 연결 성공");
										while(true)
										{
											size = client.getInputStream().read(data);
											String cmd = getWebSocketMessage(data, size);
											if(cmd != null)
											{
												Bukkit.getConsoleSender().sendMessage(Main.getMain().getTitle() + ChatColor.GOLD + client.getInetAddress().getHostAddress() + ":" + client.getPort() + ChatColor.RESET + " - " + cmd);
												Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
											}
										}
									}
									catch(Exception e)
									{
										Bukkit.getConsoleSender().sendMessage(Main.getMain().getTitle() + ChatColor.GOLD + client.getInetAddress().getHostAddress() + ":" + client.getPort() + ChatColor.YELLOW + " <-> 연결 종료");
									}
								}
								break;
							}
						}
					}
					break;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			client.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		Bukkit.getOnlinePlayers().forEach(p -> p.toString());
	}
	public void sendMessage(String msg) throws Exception
	{
		OutputStream out = client.getOutputStream();
		out.write(setWebSocketMessage(msg));
		out.flush();
	}
	public static String getWebSocketMessage(byte[] data, int len)
	{
		if((data[0] & 0xFF) == (0b10000001 & 0xFF))
		{

			int size;
			if((data[1] & 0xff) == (0b11111111  & 0xff))
			{
				size = 14;
			}
			else if((data[1] & 0xff) == (0b11111110  & 0xff))
			{
				size = 8;
			}
			else
			{
				size = 6;
			}
			byte keys[] = new byte[]{data[size - 4], data[size - 3], data[size - 2], data[size - 1]};
			try(ByteArrayOutputStream out = new ByteArrayOutputStream())
			{
				for(int i = size; i < len; i++)
				{
					out.write(data[i] ^ keys[(i - size) % keys.length]);
				}
				return new String(out.toByteArray(), Charset.forName("UTF-8"));
			}
			catch(Exception e)
			{
			}
		}
		return null;
	}
	public byte[] setWebSocketMessage(String msg)
	{
		try(ByteArrayOutputStream datas = new ByteArrayOutputStream())
		{
			byte[] data = msg.getBytes(Charset.forName("UTF-8"));
			datas.write(0b10000001);
			byte[] size = ByteBuffer.allocate(4).putInt(data.length).array();
			if(data.length <= 125)
			{
				datas.write(size[3]);
			}
			else if(data.length <= 0xFFFF)
			{
				datas.write(0b01111110);
				datas.write(size[2]);
				datas.write(size[3]);
			}
			else
			{
				datas.write(0b01111111);
				datas.write(new byte[]{0, 0, 0, 0});
				datas.write(size);
			}
			datas.write(data);
			return datas.toByteArray();
		}
		catch(Exception e)
		{
		}
		return new byte[]{(byte)0b10001010, 0};
	}
}
