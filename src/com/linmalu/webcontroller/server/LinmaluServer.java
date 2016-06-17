package com.linmalu.webcontroller.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.linmalu.library.api.LinmaluConfig;
import com.linmalu.webcontroller.Main;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;

public class LinmaluServer
{
	private HttpServer server;
	private final HashMap<ServerWebSocket, LinmaluSecurity> clients = new HashMap<>();
	private final PrintStream err = System.err;
	private final PrintStream out = System.out;
	//	private final Handler handler = new StreamHandler(out, new Formatter()
	//	{
	//		@Override
	//		public String format(LogRecord record)
	//		{
	//			
	//			return null;
	//		}
	//	});

	public LinmaluServer()
	{
		LinmaluConfig config = Main.getMain().getMainConfig();
		String port = "Port";
		if(!config.isData(port))
		{
			config.setData(port, 28282);
		}
		server = Vertx.vertx().createHttpServer(new HttpServerOptions().setPort(28282)).requestHandler(request ->
		{
			//InputStream in = getClass().getResourceAsStream("/com/linmalu/webcontroller/html/LinmaluWebController.html");
			//try(InputStream in = getClass().getResourceAsStream("/lib/" + name); FileOutputStream out = new FileOutputStream(file))
			try(InputStream in = getClass().getResourceAsStream("/html" + (request.path().equals("/") ? "/index.html" : request.path())))
			{
				byte[] data = new byte[1024];
				int size;
				Buffer buffer = Buffer.factory.buffer();
				while((size = in.read(data)) != -1)
				{
					buffer.appendBytes(data, 0, size);
				}
				request.response().end(buffer);
			}
			catch(Exception e)
			{
				request.response().setStatusCode(404).end();
			}
//			File path = new File("F:/LinmaluWebController", request.path().equals("/") ? "/index.html" : request.path());
//			if(path.exists())
//			{
//				request.response().sendFile(path.toString());
//			}
//			else
//			{
//				request.response().setStatusCode(404).end();
//			}
		}).websocketHandler(ws ->
		{
			//ws.endHandler(v -> System.out.println("끝"));
			ws.closeHandler(v ->
			{
				if(clients.containsKey(ws))
				{
					Bukkit.getConsoleSender().sendMessage(Main.getMain().getTitle() + ChatColor.GOLD + clients.remove(ws).getPlayer().getName() + ChatColor.YELLOW + "(" + ws.remoteAddress() + ")" + ChatColor.GREEN + "님이 나갔습니다.");
				}
			});
			ws.frameHandler(frame ->
			{
				try
				{
					if(clients.containsKey(ws))
					{
						LinmaluSecurity client = clients.get(ws);
						JsonObject json = new JsonParser().parse(client.decrypt(frame.textData())).getAsJsonObject();
						String type = json.get("type").getAsString();
						String msg = json.get("data").getAsString();
						OfflinePlayer player = client.getPlayer();
						if(player.isBanned())
						{
							ws.end();
						}
						if(type.equals("con") && player.isOp())
						{
							Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + "(" + ws.remoteAddress() + ")" + ChatColor.RESET + " - " + msg);
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), msg);
						}
						else if(type.equals("chat"))
						{
							String message = "<" + player.getName() + "> " + msg;
							Bukkit.broadcastMessage(message);
							sendChat(message);
						}
					}
					else
					{
						LinmaluSecurity ls = new LinmaluSecurity(frame.textData());
						if(clients.values().stream().filter(e -> e.getPlayer().getUniqueId().equals(ls.getPlayer().getUniqueId())).count() != 0)
						{
							ws.end();
						}
						else
						{
							clients.put(ws, ls);
							Bukkit.getConsoleSender().sendMessage(Main.getMain().getTitle() + ChatColor.GOLD + ls.getPlayer().getName() + ChatColor.YELLOW + "(" + ws.remoteAddress() + ")" + ChatColor.GREEN + "님이 접속했습니다.");
							sendConsoleMessage(new String(Files.readAllBytes(Paths.get("./logs/latest.log")), Charset.defaultCharset()));
						}
					}
				}
				catch(Exception e)
				{
					Bukkit.getConsoleSender().sendMessage(Main.getMain().getTitle() + ChatColor.GOLD + ws.remoteAddress() + ChatColor.YELLOW + " - 로그인에 실패했습니다.");
					ws.end();
				}
			});
		}).listen(config.getData(port, Integer.class), res ->
		{
			if(res.succeeded())
			{
				System.setErr(getPrintStream(err));
				System.setOut(getPrintStream(out));
			}
			else
			{
				Bukkit.getConsoleSender().sendMessage(Main.getMain().getTitle() + ChatColor.YELLOW + "이미 사용중인 포트이므로 웹서버가 작동하지 않습니다.");
			}
		});
	}
	public void close()
	{
		System.err.close();
		System.setErr(err);
		System.out.close();
		System.setOut(out);
		server.close();
	}
	public void sendChat(String msg)
	{
		String message = consoleToWeb(msg);
		JsonObject json = new JsonObject();
		json.addProperty("type", "chat");
		json.addProperty("data", message);
		new HashMap<>(clients).forEach((client, security) ->
		{
			try
			{
				if(!security.getPlayer().isBanned())
				{
					client.writeFinalTextFrame(security.encrypt(json.toString()));
				}
				else
				{
					client.end();
				}
			}
			catch(Exception e)
			{
				if(clients.containsKey(client))
				{
					clients.remove(client);
					Bukkit.getConsoleSender().sendMessage(Main.getMain().getTitle() + ChatColor.GOLD + client.remoteAddress() + ChatColor.YELLOW + " - 채팅 에러가 발생했습니다.");
				}
			}
		});
	}
	private void sendConsoleMessage(String msg)
	{
		if(!msg.replace("\r\n", "").equals(""))
		{
			String message = consoleToWeb("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + msg);
			JsonObject json = new JsonObject();
			json.addProperty("type", "con");
			json.addProperty("data", message);
			new HashMap<>(clients).forEach((client, security) ->
			{
				try
				{
					if(security.getPlayer().isOp() && !security.getPlayer().isBanned())
					{
						client.writeFinalTextFrame(security.encrypt(json.toString()));
					}
					else if(security.getPlayer().isBanned())
					{
						client.end();
					}
				}
				catch(Exception e)
				{
					if(clients.containsKey(client))
					{
						clients.remove(client);
						Bukkit.getConsoleSender().sendMessage(Main.getMain().getTitle() + ChatColor.GOLD + client.remoteAddress() + ChatColor.YELLOW + " - 콘솔 에러가 발생했습니다.");
					}
				}
			});
		}
	}
	private PrintStream getPrintStream(PrintStream printStream)
	{
		return new PrintStream(new ByteArrayOutputStream()
		{
			@Override
			public void flush() throws IOException
			{
				byte[] b = toByteArray();
				sendConsoleMessage(new String(b));
				printStream.write(b);
				printStream.flush();
				reset();
			}
		}, true);
	}
	private String consoleToWeb(String msg)
	{
		msg = msg.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("(", "&#40;").replace(")", "&#41;").replace("\"", "&quot;").replace("\'", "&#x27;").replace("/", "&#x2f;").replace("\r\n", "<br>").replace("\n", "<br>");
		msg = "<font>" + msg + "</font>";
		//<BLACK>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0030\u003B\u0032\u0032\u006D", "</font><font color=\"#000000\">");
		//<DARK_BLUE>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0034\u003B\u0032\u0032\u006D", "</font><font color=\"#0000AA\">");
		//<DARK_GREEN>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0032\u003B\u0032\u0032\u006D", "</font><font color=\"#00AA00\">");
		//<DARK_AQUA>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0036\u003B\u0032\u0032\u006D", "</font><font color=\"#00AAAA\">");
		//<DARK_RED>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0031\u003B\u0032\u0032\u006D", "</font><font color=\"#AA0000\">");
		//<DARK_PURPLE>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0035\u003B\u0032\u0032\u006D", "</font><font color=\"#AA00AA\">");
		//<GOLD>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0033\u003B\u0032\u0032\u006D", "</font><font color=\"#FFAA00\">");
		//<GRAY>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0037\u003B\u0032\u0032\u006D", "</font><font color=\"#AAAAAA\">");
		//<DARK_GRAY>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0030\u003B\u0031\u006D", "</font><font color=\"#555555\">");
		//<BLUE>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0034\u003B\u0031\u006D", "</font><font color=\"#5555FF\">");
		//<GREEN>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0032\u003B\u0031\u006D", "</font><font color=\"#55FF55\">");
		//<AQUA>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0036\u003B\u0031\u006D", "</font><font color=\"#55FFFF\">");
		//<RED>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0031\u003B\u0031\u006D", "</font><font color=\"#FF5555\">");
		//<LIGHT_PURPLE>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0035\u003B\u0031\u006D", "</font><font color=\"#FF55FF\">");
		//<YELLOW>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0033\u003B\u0031\u006D", "</font><font color=\"#FFFF55\">");
		//<WHITE>
		msg = msg.replace("\u001B\u005B\u0030\u003B\u0033\u0037\u003B\u0031\u006D", "</font><font color=\"#FFFFFF\">");
		//<MAGIC>
		msg = msg.replace("\u001B\u005B\u0035\u006D", "&#40;MAGIC&#41;");
		//<BOLD>
		msg = msg.replace("\u001B\u005B\u0032\u0031\u006D", "&#40;BOLD&#41;");
		//<STRIKETHROUGH>
		msg = msg.replace("\u001B\u005B\u0039\u006D", "&#40;STRIKETHROUGH&#41;");
		//<UNDERLINE>
		msg = msg.replace("\u001B\u005B\u0034\u006D", "&#40;UNDERLINE&#41;");
		//<ITALIC>
		msg = msg.replace("\u001B\u005B\u0033\u006D", "&#40;ITALIC&#41;");
		//<RESET>
		msg = msg.replace("\u001B\u005B\u006D", "</font><font>");
		return msg;
	}
}
