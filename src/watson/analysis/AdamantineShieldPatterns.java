package watson.analysis;

import java.util.regex.Pattern;

// --------------------------------------------------------------------------

/**
 * Regular expressions describing chat messages output by Prism.
 */
public interface AdamantineShieldPatterns
{
  // Adamantine Shield lookups use relative time and Minecraft's item id format
  // "0.5d - totemo broke minecraft:stone"
  // Entities get an extra word to distinguish them from players:
  // "0.5d - Entity creeper broke minecraft:stone"
  public static final Pattern PLACE_BREAK     = Pattern.compile("^((\\d+\\.\\d+)h|Near future) - (Entity )?(\\w+) (broke|placed) ((\\w+):)?(\\w+)$");

  // "0.5d - totemo added 1x minecraft:stone"
  public static final Pattern ADD_REMOVE     = Pattern.compile("^((\\d+\\.\\d+)h|Near future) - (\\w+) (added|removed) (\\d+x) ((\\w+):)?(\\w+)$");

  // Seperate logging message for liquids
  // "0.5d - Lava flow occured"
  public static final Pattern LIQUID_FLOW     = Pattern.compile("^((\\d+\\.\\d+)h|Near future) - (\\w+) flow occured$");

  // In Hover Text:
  // "Location: (-2, 65, 238)"
  public static final Pattern LOCATION        = Pattern.compile("Location: \\((-?)(\\d+), (\\d+), (-?)(\\d+)\\)");

  // [AS] Showing results, page 1/30
  // Someone may one day fix the 's' on "results" for singular results.
  public static final Pattern RESULT_PAGE = Pattern.compile("^\\[AS\\] Showing result(s)?, page (\\d+)\\/(\\d+)$");

} // class PrismPatterns