package watson.model;

import com.mumfrey.liteloader.gl.GL;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;

import watson.db.BlockType;

// ----------------------------------------------------------------------------
/**
 * A {@link BlockModel implementation that draws a block as two perpendicular
 * intersecting rectangles at 45 degrees to the X and Z coordinate axes.
 * 
 * This is, in essence, the wireframe incarnation of the billboard onto which
 * Minecraft vegetation textures are rendered.
 */
public class PlantBlockModel extends BlockModel
{
  // --------------------------------------------------------------------------
  /**
   * Default constructor.
   */
  public PlantBlockModel()
  {
    super("plant");
  }

  // --------------------------------------------------------------------------
  /**
   * @see watson.model.BlockModel#render(watson.db.BlockType, int, int, int)
   */
  @Override
  public void render(BlockType blockType, int x, int y, int z)
  {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder vb = tess.getBuffer();

    double x1 = x + blockType.getX1();
    double y1 = y + blockType.getY1();
    double z1 = z + blockType.getZ1();
    double x2 = x + blockType.getX2();
    double y2 = y + blockType.getY2();
    double z2 = z + blockType.getZ2();

    // First rectangle.
    vb.begin(GL.GL_LINE_LOOP, GL.VF_POSITION);
    GL.glColor4f(blockType.getARGB().getRed() / 255f,
            blockType.getARGB().getGreen() / 255f, blockType.getARGB().getBlue() / 255f,
            blockType.getARGB().getAlpha());
    GL.glLineWidth(blockType.getLineWidth());
    vb.pos(x1, y1, z1).endVertex();
    vb.pos(x2, y1, z2).endVertex();
    vb.pos(x2, y2, z2).endVertex();
    vb.pos(x1, y2, z1).endVertex();
    tess.draw();

    // Second rectangle.
    vb.begin(GL.GL_LINE_LOOP, GL.VF_POSITION);
    GL.glColor4f(blockType.getARGB().getRed() / 255f,
            blockType.getARGB().getGreen() / 255f, blockType.getARGB().getBlue() / 255f,
            blockType.getARGB().getAlpha());
    GL.glLineWidth(blockType.getLineWidth());
    vb.pos(x1, y1, z2).endVertex();
    vb.pos(x2, y1, z1).endVertex();
    vb.pos(x2, y2, z1).endVertex();
    vb.pos(x1, y2, z2).endVertex();
    tess.draw();
  } // render
} // class PlantBlockModel
