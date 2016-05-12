package com.linmalu.webcontroller;

import com.linmalu.library.api.LinmaluMain;
import com.linmalu.webcontroller.server.LinmaluWebServer;

public class Main extends LinmaluMain
{
	@Override
	public void onEnable()
	{
		super.onEnable();
		registerEvents(new Main_Event());
		new LinmaluWebServer();
	}
}
