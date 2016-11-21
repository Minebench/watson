package watson.cli;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import watson.Configuration;
import watson.chat.Chat;
import watson.chat.ChatHighlighter;

// --------------------------------------------------------------------------
/**
 * The Watson /hl command.
 */
public class HighlightCommand extends WatsonCommandBase
{
  // --------------------------------------------------------------------------
  /**
   * @see net.minecraft.command.ICommand#getName()
   */
  @Override
  public String getName()
  {
    return "hl";
  }

  // --------------------------------------------------------------------------
  /**
   * @see net.minecraft.command.ICommand#execute(net.minecraft.server.MinecraftServer,
   *      net.minecraft.command.ICommandSender,
   *      java.lang.String[])
   */
  @Override
  public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
  {
    if (!Configuration.instance.useChatHighlights()) {
      localOutput(sender, "Chat highlights are currently disabled. Set chat_highlights to true in the configuration to enabled them. (May be broken currently :/)");
      return;
    }
    ChatHighlighter highlighter = Chat.getChatHighlighter();
    if (args.length == 0)
    {
      help(sender);
      return;
    }
    else if (args.length == 1)
    {
      if (args[0].equalsIgnoreCase("help"))
      {
        help(sender);
        return;
      }
      else if (args[0].equalsIgnoreCase("list"))
      {
        highlighter.listHighlights();
        return;
      }
    }
    else if (args.length == 2)
    {
      if (args[0].equalsIgnoreCase("remove"))
      {
        int index = parseInt(args[1], 1);
        highlighter.removeHighlight(index);
        return;
      }
    }
    else if (args.length >= 3)
    {
      if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("select"))
      {
        // Allow patterns to contain spaces, rather than requiring \s.
        StringBuilder pattern = new StringBuilder();
        for (int i = 2; i < args.length; ++i)
        {
          pattern.append(args[i]);
          if (i < args.length - 1)
          {
            pattern.append(' ');
          }
        }
        highlighter.addHighlight(args[1], pattern.toString(),
          args[0].equalsIgnoreCase("select"));
        return;
      }
    }

    localError(sender, "Invalid command syntax.");
  } // processCommand

  // --------------------------------------------------------------------------
  /**
   * Show a help message.
   */
  public void help(ICommandSender sender)
  {
    localOutput(sender, "Usage:");
    localOutput(sender, "  /hl help");
    localOutput(sender, "  /hl add <colour> <pattern>");
    localOutput(sender, "  /hl list");
    localOutput(sender, "  /hl remove <number>");
    localOutput(sender, "Documentation: http://github.com/totemo/watson");
  }
} // class HighlightCommand