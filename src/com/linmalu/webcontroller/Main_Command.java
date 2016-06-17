package com.linmalu.webcontroller;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.linmalu.library.api.LinmaluConfig;
import com.linmalu.library.api.LinmaluTellraw;
import com.linmalu.library.api.LinmaluVersion;
import com.linmalu.webcontroller.server.LinmaluSecurity;

public class Main_Command implements CommandExecutor
{
	public Main_Command()
	{
		Main.getMain().getCommand(Main.getMain().getDescription().getName()).setTabCompleter(new TabCompleter()
		{
			@Override
			public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
			{
				if(args.length == 1)
				{
					ArrayList<String> list = new ArrayList<>();
					list.add("생성");
					list.add("삭제");
					list.add("create");
					list.add("delete");
					return list;
				}
				return null;
			}
		});
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if(sender instanceof Player)
		{
			LinmaluConfig config = Main.getMain().getPlayersConfig();
			if(args.length == 3 && (args[0].equals("생성") || args[0].equalsIgnoreCase("create")))
			{
				if(config.isData(((Player)sender).getUniqueId().toString()))
				{
					sender.sendMessage(Main.getMain().getTitle() + ChatColor.YELLOW + "이미 비밀번호가 있습니다.");
				}
				else if(!args[1].equals(args[2]))
				{
					sender.sendMessage(Main.getMain().getTitle() + ChatColor.YELLOW + "비밀번호가 일치하지 않습니다.");
				}
				else
				{
					try
					{
						config.setData(((Player)sender).getUniqueId().toString(), LinmaluSecurity.createKey(args[1]));
						sender.sendMessage(Main.getMain().getTitle() + ChatColor.GREEN + "비밀번호가 생성되었습니다." + ChatColor.GRAY + "(비밀번호는 암호화되어 확인이 불가능합니다.)");
					}
					catch(Exception e)
					{
						sender.sendMessage(Main.getMain().getTitle() + ChatColor.RED + "비밀번호생성에 실패했습니다.");
						e.printStackTrace();
					}
				}
				return true;
			}
			else if(args.length == 1 && (args[0].equals("삭제") || args[0].equalsIgnoreCase("delete")))
			{
				if(config.isData(((Player)sender).getUniqueId().toString()))
				{
					config.removeData(((Player)sender).getUniqueId().toString());
					sender.sendMessage(Main.getMain().getTitle() + ChatColor.GREEN + "비밀번호가 삭제되었습니다.");
				}
				else
				{
					sender.sendMessage(Main.getMain().getTitle() + ChatColor.YELLOW + "비밀번호가 없습니다.");
				}
				return true;
			}
		}
		sender.sendMessage(ChatColor.GREEN + " = = = = = [ Linmalu Web Controller ] = = = = =");
		LinmaluTellraw.sendChat(sender, "/" + label + " create", ChatColor.GOLD + "/" + label + " 생성/create <비밀번호> <비밀번호>" + ChatColor.GRAY + " : 비밀번호 생성");
		LinmaluTellraw.sendChat(sender, "/" + label + " create", ChatColor.GOLD + "/" + label + " 삭제/delete" + ChatColor.GRAY + " : 비밀번호 삭제");
		sender.sendMessage(ChatColor.YELLOW + "제작자 : " + ChatColor.AQUA + "린마루(Linmalu)" + ChatColor.WHITE + " - http://blog.linmalu.com");
		sender.sendMessage(ChatColor.YELLOW + "카페 : " + ChatColor.WHITE + "http://cafe.naver.com/craftproducer");
		if(sender.isOp())
		{
			LinmaluVersion.check(Main.getMain(), sender);
		}
		return true;
	}
}
