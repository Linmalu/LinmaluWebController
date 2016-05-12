package com.linmalu.webcontroller.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.linmalu.library.api.LinmaluUtility;
import com.linmalu.library.api.LinmaluYamlConfiguration;
import com.linmalu.webcontroller.Main;

public class LinmaluWebServer implements Runnable
{
	private ServerSocket server;
	private final List<LinmaluWebClient> clients = new LinkedList<>();
	private final File file = new File(Main.getMain().getDataFolder(), "config.yml");

	public LinmaluWebServer()
	{

		if(!file.exists())
		{
			LinmaluYamlConfiguration config = new LinmaluYamlConfiguration();
			config.set("Port", 28282);
			config.set("Password", LinmaluUtility.newPassword(10));
			try
			{
				config.save(file);
			}
			catch(Exception e)
			{
			}
		}
		try
		{
			server = new ServerSocket(LinmaluYamlConfiguration.loadConfiguration(file).getInt("Port", 28282));
			System.setErr(getPrintStream(System.err));
			System.setOut(getPrintStream(System.out));
			Runtime.getRuntime().addShutdownHook(new Thread(() ->
			{
				try
				{
					server.close();
				}
				catch(Exception e)
				{
				}
			}));
			new Thread(this).start();
		}
		catch(IOException e)
		{
			Bukkit.getConsoleSender().sendMessage(Main.getMain().getTitle() + ChatColor.YELLOW + "해당포트는 사용중입니다.");
		}
	}
	@Override
	public void run()
	{
		try
		{
			while(true)
			{
				clients.add(new LinmaluWebClient(server.accept()));
			}
		}
		catch(IOException e)
		{
			Bukkit.getConsoleSender().sendMessage(Main.getMain().getTitle() + ChatColor.YELLOW + "서버가 종료되었습니다.");
		}
	}
	private void messageEvent(String msg)
	{
		String message = LinmaluUtility.consoleToText(msg);
		if(!message.replace("\r\n", "").equals(""))
		{
			new ArrayList<>(clients).forEach(client ->
			{
				try
				{
					client.sendMessage(message);
				}
				catch(Exception e)
				{
					clients.remove(client);
				}
			});
		}
	}
	private PrintStream getPrintStream(PrintStream printStream)
	{
		return new PrintStream(new ByteArrayOutputStream()
		{
			private PrintStream ps = printStream;

			@Override
			public void flush() throws IOException
			{
				byte[] b = toByteArray();
				new Thread(() -> messageEvent(new String(b))).start();
				ps.write(b);
				ps.flush();
				reset();
			}
		}, true);
	}
}
