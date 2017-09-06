package watson.analysis;

import net.minecraft.util.text.ITextComponent;
import watson.Configuration;
import watson.Controller;
import watson.SyncTaskQueue;
import watson.analysis.task.AddBlockEditTask;
import watson.db.BlockEdit;
import watson.db.BlockType;
import watson.db.BlockTypeRegistry;

import java.util.Locale;
import java.util.regex.Matcher;

import static watson.analysis.AdamantineShieldPatterns.*;

// ----------------------------------------------------------------------------

/**
 * An {@link Analysis} implementation that recognises results from the results
 * of a lookup query of the AdamantineShield Sponge plugin
 * TODO: write an universal logging API plugin that supports a custom protocol
 * for querying any Minecraft logging plugin so I don't have to scrape chat. :)
 */
public class AdamantineShieldAnalysis extends Analysis
{

  public static void main(String[] args)
  {
    String line1 = " - totemo broke ironore 2m ago (a:break)";
    Matcher m = PLACE_BREAK.matcher(line1);
    if (m.matches())
    {
      for (int i = 0; i <= m.groupCount(); ++i)
      {
        System.out.println(i + ": " + m.group(i));
      }
    }
  }

  // --------------------------------------------------------------------------
  /**
   * Constructor.
   */
  public AdamantineShieldAnalysis()
  {
    addMatchedChatHandler(PLACE_BREAK, (chat, m) ->
    {
      placeBreak(chat, m);
      return true;
    });
    addMatchedChatHandler(ADD_REMOVE, (chat, m) ->
    {
      addRemove(chat, m);
      return true;
    });
    addMatchedChatHandler(RESULT_PAGE, (chat, m) ->
    {
      resultPage(chat, m);
      return true;
    });
  } // constructor

  private void resultPage(ITextComponent chat, Matcher m)
  {
    _currentPage = Integer.parseInt(m.group(2));
    _pageCount = Integer.parseInt(m.group(3));
  }

  // --------------------------------------------------------------------------
  /**
   * For AdamantineShield reports of a block being placed or broken, 
   * parse date, player name, block type, action and location.
   */
  @SuppressWarnings("unused")
  void placeBreak(ITextComponent chat, Matcher m)
  {
    long millis =  parseRelativeTime(m.group(2));
    String actor = m.group(4);
    String action = m.group(5);
    String mod = "minecraft";
    if (m.group(7) != null) {
      mod = m.group(7);
    }
    String block = m.group(8);
    BlockType type = BlockTypeRegistry.instance.getBlockTypeByName(block.replace("_", " "));
    boolean placed = action.equals("placed");

    Matcher location = LOCATION.matcher(chat.toString());

    int x = Integer.parseInt(m.group(1) != null ? "-" : "" + m.group(2));
    int y = Integer.parseInt(m.group(3));
    int z = Integer.parseInt(m.group(4) != null ? "-" : "" + m.group(5));

    BlockEdit edit = new BlockEdit(millis, actor, placed, x, y, z, type);
    SyncTaskQueue.instance.addTask(new AddBlockEditTask(edit, true));

    requestNextPage();
  } // placeBreak

  // --------------------------------------------------------------------------
  /**
   * For AdamantineShield reports of an interaction with an inventory block
   * and treat it like block breaks/places
   * parse date, player name, action and location.
   */
  @SuppressWarnings("unused")
  void addRemove(ITextComponent chat, Matcher m)
  {
    long millis = parseRelativeTime(m.group(2));
    String actor = m.group(3);
    String action = m.group(4);
    String mod = "minecraft";
    if (m.group(7) != null) {
      mod = m.group(7); // Currently unused. Might want to use that in the future to properly support mods
    }
    String block = m.group(8);
    BlockType type = BlockTypeRegistry.instance.getBlockTypeByName(block.replace("_", " "));
    boolean added = action.equals("added");

    Matcher location = LOCATION.matcher(chat.toString());

    int x = Integer.parseInt(m.group(1) != null ? "-" : "" + m.group(2));
    int y = Integer.parseInt(m.group(3));
    int z = Integer.parseInt(m.group(4) != null ? "-" : "" + m.group(5));

    BlockEdit edit = new BlockEdit(millis, actor, added, x, y, z, type);
    SyncTaskQueue.instance.addTask(new AddBlockEditTask(edit, true));

    requestNextPage();
  } // addRemove

  // --------------------------------------------------------------------------

  /**
   * Parses "time ago" messages to an absolute unix timestamp
   * @param time The time string. Should always be a double representing the hours ago
   *             or can also be <tt>null</tt> to represent an edit in the near future
   * @return The absolute unix time stamp
   */
  private long parseRelativeTime(String time)
  {
    if (time != null)
    {
      return System.currentTimeMillis() - (long) (Double.parseDouble(time) * 60 * 60 * 1000);
    }
    return System.currentTimeMillis();
  } // parseRelativeTime

  // --------------------------------------------------------------------------
  /**
   * This method is called when coordinates are parsed out of chat to request
   * the next page of "/lb coords" results, up to the configured maximum number
   * of pages.
   */
  private void requestNextPage()
  {
    if (Configuration.instance.isAutoPage())
    {
      if (_currentPage != 0 && _currentPage < _pageCount
          && _pageCount <= Configuration.instance.getMaxAutoPages())
      {
        Controller.instance.serverChat(String.format(Locale.US, "/ashield page %d", _currentPage + 1));

        // Remember that we don't need to do this again until next page is
        // parsed.
        _currentPage = _pageCount = 0;
      }
    }
  } // requestNextPage

  /**
   * Current page number extracted from lb.page lines.
   */
  protected int                _currentPage            = 0;

  /**
   * Total number of pages of results, from lb.page lines.
   */
  protected int                _pageCount              = 0;

} // class AdamantineShieldAnalysis