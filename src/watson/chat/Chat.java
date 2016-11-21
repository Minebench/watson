package watson.chat;

import java.util.logging.Level;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import watson.Configuration;
import watson.debug.Log;

// ----------------------------------------------------------------------------
/**
 * Handles local and to-server chat, including highlighting of received chat
 * messages.
 */
public class Chat
{
  // --------------------------------------------------------------------------
  /**
   * Return the (@link ChatHighlighter} that colours naughty words and whatnot
   * in chat lines.
   * 
   * @return the {@link ChatHighlighter}.
   */
  public static ChatHighlighter getChatHighlighter()
  {
    return _chatHighlighter;
  }

  // --------------------------------------------------------------------------
  /**
   * Send a chat message to the server.
   * 
   * @param message the text to display.
   */
  public static void serverChat(String message)
  {
    try
    {
      Minecraft mc = Minecraft.getMinecraft();
      mc.player.sendChatMessage(message);
    }
    catch (Exception ex)
    {
      Log.exception(Level.SEVERE, "Sending chat to the server.", ex);
    }
  }

  // --------------------------------------------------------------------------
  /**
   * Return true if the chat GUI is ready to display chat.
   * 
   * @return true if the chat GUI is ready to display chat.
   */
  public static boolean isChatGuiReady()
  {
    Minecraft mc = Minecraft.getMinecraft();
    return mc.ingameGUI != null && mc.ingameGUI.getChatGUI() != null;
  }

  // --------------------------------------------------------------------------
  /**
   * Display a chat message locally.
   * 
   * @param message the text to display.
   */
  public static void localChat(String message)
  {
    localChat(new TextComponentString(message));
  }

  // --------------------------------------------------------------------------
  /**
   * Display a chat message locally.
   * 
   * @param colour the colour to format the text as.
   * @param message the text to display.
   */
  public static void localChat(TextFormatting colour, String message)
  {
    TextComponentString chat = new TextComponentString(message);
    Style style = new Style();
    style.setColor(colour);
    chat.setStyle(style);
    localChat(chat);
  }

  // --------------------------------------------------------------------------
  /**
   * Display the chat locally.
   *
   * @param chat the chat component.
   */
  public static void localChat(ITextComponent chat)
  {
    if (isChatGuiReady())
    {
      ITextComponent highlighted = Configuration.instance.useChatHighlights() ? getChatHighlighter().highlight(chat) : chat;
      Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(highlighted);
    }
  }

  // --------------------------------------------------------------------------
  /**
   * Display the output of Watson commands, locally in the client.
   * 
   * @param message the text to display.
   */
  public static void localOutput(String message)
  {
    localChat(TextFormatting.AQUA, message);
  }

  // --------------------------------------------------------------------------
  /**
   * Display Watson error messages, locally in the client.
   * 
   * @param message the text to display.
   */
  public static void localError(String message)
  {
    localChat(TextFormatting.DARK_RED, message);
  }

  // --------------------------------------------------------------------------
  /**
   * The chat highlighter.
   */
  protected static ChatHighlighter _chatHighlighter = new ChatHighlighter();
} // class Chat