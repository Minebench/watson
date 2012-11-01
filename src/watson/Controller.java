package watson;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.logging.Level;

import net.minecraft.client.Minecraft;
import net.minecraft.src.GuiIngame;
import net.minecraft.src.GuiNewChat;
import net.minecraft.src.ModLoader;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.mod_ClientCommands;
import net.minecraft.src.mod_Watson;
import watson.analysis.Sherlock;
import watson.chat.ChatProcessor;
import watson.cli.AnnoCommand;
import watson.cli.CalcCommand;
import watson.cli.CaseInsensitivePrefixFileFilter;
import watson.cli.HighlightCommand;
import watson.cli.TagCommand;
import watson.cli.WatsonCommand;
import watson.debug.Log;

// ----------------------------------------------------------------------------
/**
 * Provides a centralised Facade to control the facilities of this mod.
 */
public class Controller
{
  // --------------------------------------------------------------------------
  /**
   * Singleton.
   */
  public static final Controller instance = new Controller();

  // --------------------------------------------------------------------------
  /**
   * Mod-wide initialisation tasks:
   * <ul>
   * <li>loading chat categories</li>
   * <li>initialising Sherlock</li>
   * <li>loading block types</li>
   * </ul>
   */
  public void initialise()
  {
    ChatProcessor.getInstance().loadChatCategories();
    ChatProcessor.getInstance().loadChatExclusions();
    _sherlock = new Sherlock(ChatProcessor.getInstance().getChatClassifier());
    BlockTypeRegistry.instance.loadBlockTypes();
    _chatHighlighter.loadHighlights();

    // Initialise the commands.
    mod_ClientCommands.getInstance().registerCommand(new WatsonCommand());
    mod_ClientCommands.getInstance().registerCommand(new AnnoCommand());
    mod_ClientCommands.getInstance().registerCommand(new TagCommand());
    mod_ClientCommands.getInstance().registerCommand(new HighlightCommand());
    mod_ClientCommands.getInstance().registerCommand(new CalcCommand());
  }

  // --------------------------------------------------------------------------
  /**
   * Return the Sherlock instance.
   * 
   * @return the Sherlock instance.
   */
  public Sherlock getSherlock()
  {
    return _sherlock;
  }

  // --------------------------------------------------------------------------
  /**
   * Return the (@link ChatHighlighter} that colours naughty words and whatnot
   * in chat lines.
   * 
   * @return the {@link ChatHighlighter}.
   */
  public ChatHighlighter getChatHighlighter()
  {
    return _chatHighlighter;
  }

  // --------------------------------------------------------------------------
  /**
   * Return the {@link DisplaySettings} which control what is drawn.
   * 
   * @return the {@link DisplaySettings} which control what is drawn.
   */
  public DisplaySettings getDisplaySettings()
  {
    return _displaySettings;
  }

  // --------------------------------------------------------------------------
  /**
   * Return the current {@link BlockEditSet} under examination.
   * 
   * A separate {@link BlockEditSet} is maintained for each dimension
   * (overworld, nether, end).
   * 
   * @return the current {@link BlockEditSet} under examination.
   */
  public BlockEditSet getBlockEditSet()
  {
    // Compute id of the form: address/dimension
    // Note: Minecraft.theWorld.getWorldInfo().getDimension() doesn't update.
    Minecraft mc = ModLoader.getMinecraftInstance();
    StringBuilder idBuilder = new StringBuilder();
    if (!mc.isSingleplayer())
    {
      idBuilder.append(mc.getServerData().serverIP);
    }
    idBuilder.append('/');
    idBuilder.append(mc.thePlayer.dimension);
    String id = idBuilder.toString();

    // Lookup BlockEditSet or create new mapping if not found.
    BlockEditSet edits = _edits.get(id);
    if (edits == null)
    {
      edits = new BlockEditSet();
      _edits.put(id, edits);
    }
    return edits;
  } // getBlockEditSet

  // --------------------------------------------------------------------------
  /**
   * Save the current {@link BlockEditSet} to the specified file in
   * getSaveDirectory().
   * 
   * @param fileName the file name to write; if it is null and there is a
   *          current player variable value, a default file name of the form
   *          player-YYYY-MM-DD-hh.mm.ss is used.
   * 
   */
  public void saveBlockEditFile(String fileName)
  {
    // Compute default fileName?
    if (fileName == null)
    {
      String player = (String) getVariables().get("player");
      if (player == null)
      {
        localError("No current player set, so you must specify a file name.");
        return;
      }
      else
      {
        Calendar calendar = Calendar.getInstance();
        fileName = String.format("%s-%4d-%02d-%02d-%02d.%02d.%02d", player,
          calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
          calendar.get(Calendar.DAY_OF_MONTH),
          calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
          calendar.get(Calendar.SECOND));
      }
    } // if

    createBlockEditDirectory();

    File file = new File(getBlockEditDirectory(), fileName);
    try
    {
      BlockEditSet edits = getBlockEditSet();
      int editCount = edits.save(file);
      int annoCount = edits.getAnnotations().size();
      localChat(String.format("Saved %d edits and %d annotations to %s",
        editCount, annoCount, fileName));
    }
    catch (IOException ex)
    {
      Log.exception(Level.SEVERE, "error saving BlockEditSet to " + file, ex);
      localError("The file " + fileName + " could not be saved.");
    }
  } // saveBlockEditFile

  // --------------------------------------------------------------------------
  /**
   * Load the set of {@link BlockEdit}s from the specified file.
   * 
   * @param fileName the file name, or the start of the file name (beginning of
   *          player name), in the BlockEdit saves directory.
   * 
   * @TODO: Does this need to be smarter about which dimension/server we're in?
   */
  public void loadBlockEditFile(String fileName)
  {
    File file = new File(getBlockEditDirectory(), fileName);
    if (!file.canRead())
    {
      // Try to find a file that begins with fileName, i.e. treat that as the
      // player name.
      File[] files = getBlockEditFileList(fileName);
      if (files.length > 0)
      {
        // Chose the most recent matching file.
        file = files[files.length - 1];
      }
    }

    if (file.canRead())
    {
      try
      {
        BlockEditSet edits = getBlockEditSet();
        int editCount = edits.load(file);
        int annoCount = edits.getAnnotations().size();
        localChat(String.format("Loaded %d edits and %d annotations from %s",
          editCount, annoCount, file.getName()));
      }
      catch (Exception ex)
      {
        Log.exception(Level.SEVERE, "error loading BlockEditSet from " + file,
          ex);
        localError("The file " + fileName + " could not be loaded.");
      }
    }
    else
    {
      localError("Can't open " + fileName + " to read.");
    }
  } // loadBlockEditFile

  // --------------------------------------------------------------------------
  /**
   * List all of the {@link BlockEditSet} save files whose names begin with the
   * specified prefix, matched case-insensitively.
   */
  public void listBlockEditFiles(String prefix)
  {
    File[] files = getBlockEditFileList(prefix);
    localChat(files.length + " matching file(s):");
    for (File file : files)
    {
      localChat(file.getName());
    }
  } // listBlockEditFiles

  // --------------------------------------------------------------------------
  /**
   * Return an array of {@link BlockEditSet} save files whose names begin with
   * the specified prefix, matched case insensitively.
   * 
   * @param prefix the case-insensitive prefix.
   * @return the array of files.
   */
  public File[] getBlockEditFileList(String prefix)
  {
    File[] files = getBlockEditDirectory().listFiles(
      new CaseInsensitivePrefixFileFilter(prefix));
    Arrays.sort(files);
    return files;
  }

  // --------------------------------------------------------------------------
  /**
   * Clear the BlockEditSet for the current server and dimension.
   * 
   * Also clear the variables scraped from chat.
   */
  public void clearBlockEditSet()
  {
    getBlockEditSet().clear();
    _variables.clear();
    localChat("Watson edits cleared.");
  }

  // --------------------------------------------------------------------------
  /**
   * Issue a LogBlock query that selects the edits that came immediately before
   * the most recent coalblock, /lb query result, or /lb tp destination. The
   * query takes the form:
   * 
   * <pre>
   * /lb before MM-DD hh:mm:ss player name coords limit 45
   * </pre>
   * 
   * This method is called in response to the "/w pre" command.
   */
  public void queryPreviousEdits()
  {
    if (_variables.containsKey("player") && _variables.containsKey("time"))
    {
      _calendar.setTimeInMillis((Long) _variables.get("time"));
      int day = _calendar.get(Calendar.DAY_OF_MONTH);
      int month = _calendar.get(Calendar.MONTH) + 1;
      int year = _calendar.get(Calendar.YEAR);
      int hour = _calendar.get(Calendar.HOUR_OF_DAY);
      int minute = _calendar.get(Calendar.MINUTE);
      int second = _calendar.get(Calendar.SECOND);
      String player = (String) _variables.get("player");

      String query = String.format(
        "/lb before %d.%d.%d %02d:%02d:%02d player %s coords limit 45", day,
        month, year, hour, minute, second, player);
      Log.debug(query);
      serverChat(query);
    }
  } // queryPreviousEdits

  // --------------------------------------------------------------------------
  /**
   * Get a mutable reference to the Map of all of the variables scraped from
   * chat lines.
   * 
   * @return the variables.
   */
  public HashMap<String, Object> getVariables()
  {
    return _variables;
  }

  // --------------------------------------------------------------------------
  /**
   * Display the specified chat message in the local client's chat GUI.
   * 
   * @param message the chat message to display.
   */
  public void localChat(String message)
  {
    System.out.println(message);
    if (getChatGui() != null)
    {
      getChatGui().printChatMessage(_chatHighlighter.highlight(message));
    }
  }

  // --------------------------------------------------------------------------
  /**
   * Display the specified error message in bright red in the local client's
   * chat GUI.
   * 
   * TODO: check with people with various types of colour blindness to see if
   * this is ok by them or causes problems.
   * 
   * @param message the chat message to display.
   */
  public void localError(String message)
  {
    if (getChatGui() != null)
    {
      getChatGui().printChatMessage("§4" + message);
    }
  }

  // --------------------------------------------------------------------------
  /**
   * Send the specified message in a chat packet to the server.
   * 
   * @param message the chat message to send.
   */
  public void serverChat(String message)
  {
    Packet3Chat chat = new Packet3Chat(message);
    ModLoader.clientSendPacket(chat);
  }

  // --------------------------------------------------------------------------
  /**
   * Private constructor to enforce Singleton pattern.
   */
  private Controller()
  {
    // Nothing.
  }

  // --------------------------------------------------------------------------
  /**
   * Return the cached reference to the Minecraft GuiNewChat instance that draws
   * the chat GUI, or null if not yet available.
   * 
   * @return the cached reference to the Minecraft GuiNewChat instance that
   *         draws the chat GUI, or null if not yet available.
   */
  private GuiNewChat getChatGui()
  {
    if (_chatGui == null)
    {
      Minecraft mc = ModLoader.getMinecraftInstance();
      if (mc != null)
      {
        GuiIngame ingame = mc.ingameGUI;
        if (ingame != null)
        {
          _chatGui = ingame.getChatGUI();
        }
      }
    }
    return _chatGui;
  } // getChatGui

  // --------------------------------------------------------------------------
  /**
   * Create the mod-specific subdirectory and subdirectories of that.
   */
  public static void createDirectories()
  {
    File modDir = getModDirectory();
    if (!modDir.isDirectory())
    {
      try
      {
        modDir.mkdirs();
      }
      catch (Exception ex)
      {
        Log.exception(Level.SEVERE,
          "could not create mod directory: " + modDir, ex);
      }
    }
  } // createDirectories

  // --------------------------------------------------------------------------
  /**
   * Ensure that the BlockEditSet saves directory exists.
   */
  public static void createBlockEditDirectory()
  {
    File dir = getBlockEditDirectory();
    if (!dir.isDirectory())
    {
      try
      {
        dir.mkdirs();
      }
      catch (Exception ex)
      {
        Log.exception(Level.SEVERE, "could not create saves directory: " + dir,
          ex);
      }
    }
  } // createBlockEditDirectory

  // --------------------------------------------------------------------------
  /**
   * Return the directory where this mod's data files are stored.
   * 
   * @return the directory where this mod's data files are stored.
   */
  public static File getModDirectory()
  {
    File minecraftDir = Minecraft.getMinecraftDir();
    return new File(minecraftDir, MOD_SUBDIR);
  }

  // --------------------------------------------------------------------------
  /**
   * Return the directory where BlockEditSet files are saved.
   * 
   * @return the directory where BlockEditSet files are saved.
   */
  public static File getBlockEditDirectory()
  {
    return new File(getModDirectory(), SAVE_SUBDIR);
  }

  // --------------------------------------------------------------------------
  /**
   * Return an input stream that reads the specified file or resource name.
   * 
   * If the file exists in the mod-specific configuration directory, it is
   * loaded from there. Otherwise, the resource of the same name is loaded from
   * the minecraft.jar file.
   * 
   * @return an input stream that reads the specified file or resource name.
   */
  public static InputStream getConfigurationStream(String fileName)
    throws IOException
  {
    File file = new File(Controller.getModDirectory(), fileName);
    if (file.canRead())
    {
      Log.info("Loading \"" + fileName + "\" from file.");
      return new BufferedInputStream(new FileInputStream(file));
    }
    else
    {
      Log.info("Loading \"" + fileName + "\" from resource in minecraft.jar.");
      ClassLoader loader = mod_Watson.class.getClassLoader();
      return loader.getResourceAsStream(Controller.MOD_PACKAGE + '/' + fileName);
    }
  } // getConfigurationStream

  // --------------------------------------------------------------------------
  /**
   * Makes inferences based on LogBlock query results.
   */
  protected Sherlock                      _sherlock;

  /**
   * The settings affecting what is displayed and how.
   */
  protected DisplaySettings               _displaySettings = new DisplaySettings();

  /**
   * A map from the a String containing the server address and dimension number
   * to the corresponding set of {@link BlockEdit}s that are displayed by
   * {@link RenderWatson}.
   */
  protected HashMap<String, BlockEditSet> _edits           = new HashMap<String, BlockEditSet>();

  /**
   * A cached reference to the GuiNewChat instance, set up as soon as it becomes
   * available.
   */
  protected GuiNewChat                    _chatGui;

  /**
   * The chat highlighter.
   */
  protected ChatHighlighter               _chatHighlighter = new ChatHighlighter();

  /**
   * Map from name to value of all of the variables scraped from chat lines.
   */
  protected HashMap<String, Object>       _variables       = new HashMap<String, Object>();

  /**
   * Used to compute time stamps for queryPreviousEdits().
   */
  Calendar                                _calendar        = Calendar.getInstance();

  /**
   * The main package name of the classes of this mod, and also the name of the
   * subdirectory of .minecraft/mods/ where mod-specific settings are stored.
   */
  private static final String             MOD_PACKAGE      = "watson";

  /**
   * Directory where mod files reside, relative to the .minecraft/ directory.
   */
  private static final String             MOD_SUBDIR       = "mods"
                                                             + File.separator
                                                             + MOD_PACKAGE;
  /**
   * Subdirectory of the mod specific directory where {@link BlockEditSet}s are
   * saved.
   */
  private static final String             SAVE_SUBDIR      = "saves";
} // class Controller