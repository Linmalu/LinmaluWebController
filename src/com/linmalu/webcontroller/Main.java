package com.linmalu.webcontroller;

import java.io.File;

import com.linmalu.library.api.LinmaluConfig;
import com.linmalu.library.api.LinmaluMain;
import com.linmalu.webcontroller.server.LinmaluServer;

public class Main extends LinmaluMain
{
	public static Main getMain()
	{
		return (Main)LinmaluMain.getMain();
	}

	private final LinmaluConfig mainConfig = new LinmaluConfig(new File(getDataFolder(), "config.yml"));
	private final LinmaluConfig playersConfig = new LinmaluConfig(new File(getDataFolder(), "players.yml"));
	private LinmaluServer server;

	@Override
	public void onEnable()
	{
		super.onEnable();
		registerCommand(new Main_Command());
		registerEvents(new Main_Event());
		registerLibrary("vertx-core-3.5.0.jar");
		server = new LinmaluServer();
	}
	@Override
	public void onDisable()
	{
		super.onDisable();
		server.close();
	}
	public LinmaluConfig getMainConfig()
	{
		return mainConfig;
	}
	public LinmaluConfig getPlayersConfig()
	{
		return playersConfig;
	}
	public LinmaluServer getLinmaluServer()
	{
		return server;
	}
}
