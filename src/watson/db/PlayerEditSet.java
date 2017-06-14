package watson.db;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TreeSet;

import com.mumfrey.liteloader.gl.GL;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;

import net.minecraft.util.math.Vec3d;

import watson.Controller;
import watson.DisplaySettings;
import watson.model.ARGB;

// ----------------------------------------------------------------------------
/**
 * Maintains a time-ordered list of all of the BlockEdit instances corresponding
 * to LogBlock results for one player only, ordered from oldest to most recent.
 */
public class PlayerEditSet
{
  // --------------------------------------------------------------------------
  /**
   * Constructor.
   *
   * @param player name of the player who did these edits.
   */
  public PlayerEditSet(String player)
  {
    _player = player;
  }

  // --------------------------------------------------------------------------
  /**
   * Return the name of the player who did these edits.
   *
   * @return the name of the player who did these edits.
   */
  public String getPlayer()
  {
    return _player;
  }

  // --------------------------------------------------------------------------
  /**
   * Find an edit with the specified coordinates.
   *
   * Currently, this method does an inefficient linear search, walking, on
   * average, half the collection. The edits are searched from oldest to newest,
   * meaning that the oldest edit at that coordinate will be retrieved.
   *
   * TODO: implement an efficient spatial search.
   *
   * @param x the x coordinate of the block
   * @param y the y coordinate of the block
   * @param z the z coordinate of the block
   * @return the matching edit, or null if not found.
   */
  public synchronized BlockEdit findEdit(int x, int y, int z)
  {
    // Warning: O(N) algorithm. Avert your delicate eyes.
    for (BlockEdit edit : _edits)
    {
      if (edit.x == x && edit.y == y && edit.z == z)
      {
        return edit;
      }
    }
    return null;
  }

  // --------------------------------------------------------------------------
  /**
   * Return the edit before the specified edit, or null if there is no edit
   * before.
   *
   * @param edit the edit.
   * @return the edit before the specified edit, or null if there is no edit
   *         before.
   */
  public synchronized BlockEdit getEditBefore(BlockEdit edit)
  {
    return _edits.lower(edit);
  }

  // --------------------------------------------------------------------------
  /**
   * Return the edit after the specified edit, or null if there is no edit
   * before.
   *
   * @param edit the edit.
   * @return the edit after the specified edit, or null if there is no edit
   *         before.
   */
  public synchronized BlockEdit getEditAfter(BlockEdit edit)
  {
    return _edits.higher(edit);
  }

  // --------------------------------------------------------------------------
  /**
   * Add the specified edit to the list.
   *
   * @param edit the BlockEdit describing an edit to add.
   */
  public synchronized void addBlockEdit(BlockEdit edit)
  {
    _edits.add(edit);

    // Reference container for fast visibility toggling of ore deposit labels.
    edit.playerEditSet = this;
  }

  // --------------------------------------------------------------------------
  /**
   * Return the number of edits stored.
   *
   * @return the number of edits stored.
   */
  public synchronized int getBlockEditCount()
  {
    return _edits.size();
  }

  // --------------------------------------------------------------------------
  /**
   * Set the visibility of this player's edits in the dimension to which this
   * PlayerEditSet applies.
   *
   * @param visible if true, edits are visible.
   */
  public void setVisible(boolean visible)
  {
    _visible = visible;
  }

  // --------------------------------------------------------------------------
  /**
   * Return the visibility of this player's edits in the dimension to which this
   * PlayerEditSet applies.
   *
   * @return the visibility of this player's edits in the dimension to which
   *         this PlayerEditSet applies.
   */
  public boolean isVisible()
  {
    return _visible;
  }

  // --------------------------------------------------------------------------
  /**
   * Draw wireframe outlines of all blocks.
   */
  public synchronized void drawOutlines()
  {
    if (isVisible())
    {
      if (Controller.instance.getDisplaySettings().isOutlineShown())
      {
        for (BlockEdit edit : _edits)
        {
          edit.drawOutline();
        }
      }
    }
  }

  // --------------------------------------------------------------------------
  /**
   * Draw direction vectors indicating motion of the miner.
   *
   * @param colour the colour to draw the vectors.
   */
  public synchronized void drawVectors(ARGB colour)
  {
    DisplaySettings settings = Controller.instance.getDisplaySettings();
    if (settings.areVectorsShown() && isVisible() && !_edits.isEmpty())
    {
      final Tessellator tess = Tessellator.getInstance();
      final BufferBuilder vb = tess.getBuffer();
      vb.begin(GL.GL_LINES, GL.VF_POSITION);

      // TODO: Make the vector colour and thickness configurable.
      GL.glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, colour.getAlpha());
      GL.glLineWidth(0.5f);

      // Unit X and Y vectors used for cross products to get arrow axes.
      Vec3d unitX = new Vec3d(1, 0, 0);
      Vec3d unitY = new Vec3d(0, 1, 0);

      // We only need to draw vectors if there are at least 2 edits.
      Iterator<BlockEdit> it = _edits.iterator();
      if (it.hasNext())
      {
        BlockEdit prev = it.next();
        while (it.hasNext())
        {
          BlockEdit next = it.next();

          // Work out whether to link edits with vectors.
          boolean show = (next.creation && settings.isLinkedCreations()) ||
                         (!next.creation && settings.isLinkedDestructions());
          if (show)
          {
            Vec3d pPos = new Vec3d(0.5 + prev.x, 0.5 + prev.y, 0.5 + prev.z);
            Vec3d nPos = new Vec3d(0.5 + next.x, 0.5 + next.y, 0.5 + next.z);
            // Vector difference, from prev to next.
            Vec3d diff = nPos.subtract(pPos);

            // Compute length. We want to scale the arrow heads by the length,
            // so can't avoid the sqrt() here.
            double length = diff.lengthVector();
            if (length >= settings.getMinVectorLength())
            {
              // Draw the vector.
              vb.pos(pPos.x, pPos.y, pPos.z).endVertex();
              vb.pos(nPos.x, nPos.y, nPos.z).endVertex();

              // Length from arrow tip to midpoint of vector as a fraction of
              // the total vector length. Scale the arrow in proportion to the
              // square root of the length up to a maximum size.
              double arrowSize = UNIT_VECTOR_ARROW_SIZE * Math.sqrt(length);
              if (arrowSize > MAX_ARROW_SIZE)
              {
                arrowSize = MAX_ARROW_SIZE;
              }
              double arrowScale = arrowSize / length;

              // Position of the tip and tail of the arrow, sitting in the
              // middle of the vector.
              Vec3d tip = new Vec3d(pPos.x * (0.5 - arrowScale) + nPos.x * (0.5 + arrowScale),
                                  pPos.y * (0.5 - arrowScale) + nPos.y * (0.5 + arrowScale),
                                  pPos.z * (0.5 - arrowScale) + nPos.z * (0.5 + arrowScale));
              Vec3d tail = new Vec3d(pPos.x * (0.5 + arrowScale) + nPos.x * (0.5 - arrowScale),
                                   pPos.y * (0.5 + arrowScale) + nPos.y * (0.5 - arrowScale),
                                   pPos.z * (0.5 + arrowScale) + nPos.z * (0.5 - arrowScale));

              // Fin axes, perpendicular to vector. Scale by vector length.
              // If the vector is colinear with the Y axis, use the X axis for
              // the cross products to derive the fin directions.
              Vec3d fin1;
              if (Math.abs(unitY.dotProduct(diff)) > 0.9 * length)
              {
                fin1 = unitX.crossProduct(diff).normalize();
              }
              else
              {
                fin1 = unitY.crossProduct(diff).normalize();
              }

              Vec3d fin2 = fin1.crossProduct(diff).normalize();

              Vec3d draw1 = new Vec3d(fin1.x * arrowScale * length,
                                    fin1.y * arrowScale * length,
                                    fin1.z * arrowScale * length);
              Vec3d draw2 = new Vec3d(fin2.x * arrowScale * length,
                                    fin2.y * arrowScale * length,
                                    fin2.z * arrowScale * length);

              // Draw four fins.
              vb.pos(tip.x, tip.y, tip.z).endVertex();
              vb.pos(tail.x + draw1.x, tail.y + draw1.y, tail.z + draw1.z).endVertex();
              vb.pos(tip.x, tip.y, tip.z).endVertex();
              vb.pos(tail.x - draw1.x, tail.y - draw1.y, tail.z - draw1.z).endVertex();
              vb.pos(tip.x, tip.y, tip.z).endVertex();
              vb.pos(tail.x + draw2.x, tail.y + draw2.y, tail.z + draw2.z).endVertex();
              vb.pos(tip.x, tip.y, tip.z).endVertex();
              vb.pos(tail.x - draw2.x, tail.y - draw2.y, tail.z - draw2.z).endVertex();
            } // if we are drawing this vector
            prev = next;
          } // if
        } // while
        tess.draw();
      } // if
    } // if drawing
  } // drawVectors

  // --------------------------------------------------------------------------
  /**
   * Write the edits for this player to the specified PrintWriter.
   *
   * @param writer the PrintWriter.
   * @return the number of edits saved.
   */
  public synchronized int save(PrintWriter writer)
  {
    Calendar calendar = Calendar.getInstance();
    int editCount = 0;
    for (BlockEdit edit : _edits)
    {
      calendar.setTimeInMillis(edit.time);
      int year = calendar.get(Calendar.YEAR);
      int month = calendar.get(Calendar.MONTH) + 1;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      int hour = calendar.get(Calendar.HOUR_OF_DAY);
      int minute = calendar.get(Calendar.MINUTE);
      int second = calendar.get(Calendar.SECOND);
      char action = edit.creation ? 'c' : 'd';
      writer.format("%4d-%02d-%02d|%02d:%02d:%02d|%s|%c|%d|%d|%d|%d|%d\n",
                    year, month, day, hour, minute, second, edit.player, action,
                    edit.type.getId(), edit.type.getData(), edit.x, edit.y, edit.z);
      ++editCount;
    } // for
    return editCount;
  } // save

  // --------------------------------------------------------------------------
  /**
   * The name of the player who did these edits.
   */
  protected String              _player;

  /**
   * A set of BlockEdit instances, ordered from oldest (lowest time value) to
   * most recent.
   */
  protected TreeSet<BlockEdit>  _edits                 = new TreeSet<BlockEdit>(new BlockEditComparator());

  /**
   * True if this player's edits are visible.
   */
  protected boolean             _visible               = true;

  /**
   * Size of the arrow on a unit length vector.
   */
  protected static final double UNIT_VECTOR_ARROW_SIZE = 0.025;

  /**
   * Maximum size of an arrow in world units.
   */
  protected static final double MAX_ARROW_SIZE         = 0.5;
} // class PlayerEditSet