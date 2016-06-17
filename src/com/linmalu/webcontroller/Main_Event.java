package com.linmalu.webcontroller;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.linmalu.library.api.LinmaluVersion;

public class Main_Event implements Listener
{
	@EventHandler
	public void Event(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		if(player.isOp())
		{
			LinmaluVersion.check(Main.getMain(), player);
		}
	}
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void Event(AsyncPlayerChatEvent event)
	{
		Main.getMain().getLinmaluServer().sendChat("<" + event.getPlayer().getName() + "> " + event.getMessage());
	}
}
