package watson.cli;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import watson.chat.Chat;

// ----------------------------------------------------------------------------
/**
 * An ICommandSender implementation for client-side command handling.
 * 
 * Most methods simply defer to Minecraft.player's handling, but for
 * client-side commands we need to allow the sender to use any command that is
 * registered with the {@link ClientCommandManager}.
 */
public class ClientCommandSender implements ICommandSender
{
  // --------------------------------------------------------------------------
  /**
   * Constructor.
   *
   * @param ccm the {@link ClientCommandManager} with which client-side commands
   *          are registered.
   */
  public ClientCommandSender(ClientCommandManager ccm)
  {
    _ccm = ccm;
    _sender = Minecraft.getMinecraft().player;
  }

  // --------------------------------------------------------------------------
  /**
   * @see net.minecraft.command.ICommandSender#getName()
   */
  @Override
  public String getName()
  {
    return _sender.getName();
  }

  // --------------------------------------------------------------------------
  /**
   * @see net.minecraft.command.ICommandSender#canUseCommand(int, java.lang.String)
   */
  @Override
  public boolean canUseCommand(int commandLevel, String command)
  {
    return true;
  }

  // --------------------------------------------------------------------------
  /**
   * Vanilla class ChatComponentTranslation extracts the ChatStyle out of this
   * IChatComponent and uses it to set the style of translated text.
   *
   * @see net.minecraft.command.ICommandSender#getDisplayName()
   */
  @Override
  public ITextComponent getDisplayName()
  {
    // ChatComponentStyle.getChatStyle() creates a default ChatStyle instance on
    // demand, so a default ChatComponentText instance suffices.
    // TODO: correct, or should this string be player name?
    return new TextComponentString("");
  }

  // --------------------------------------------------------------------------
  /**
   * @see net.minecraft.command.ICommandSender#sendMessage(net.minecraft.util.text.ITextComponent)
   */
  @Override
  public void sendMessage(ITextComponent chat)
  {
    Chat.localChat(chat);
  }

  // --------------------------------------------------------------------------
  /**
   * @see net.minecraft.command.ICommandSender#getEntityWorld()
   */
  @Override
  public World getEntityWorld()
  {
    return Minecraft.getMinecraft().player.getEntityWorld();
  }

    /**
     * Returns the entity associated with the command sender. MAY BE NULL!
     */
    @Override
    public Entity getCommandSenderEntity() {
        return Minecraft.getMinecraft().player;
    }

    /**
     * Returns true if the command sender should be sent feedback about executed commands
     */
    @Override
    public boolean sendCommandFeedback() {
        return false;
    }

    @Override
    public void setCommandStat(CommandResultStats.Type type, int amount) {

    }

  @Override
  public MinecraftServer getServer() {
    return null;
  }

  // --------------------------------------------------------------------------
  /**
   * @see net.minecraft.command.ICommandSender#getPosition()
   */
  @Override
  public BlockPos getPosition()
  {
    return Minecraft.getMinecraft().player.getPosition();
  }

  // --------------------------------------------------------------------------

    @Override
    public Vec3d getPositionVector() {
        return Minecraft.getMinecraft().player.getPositionVector();
    }

  /**
   * The default sender - Minecraft.player.
   */
  protected ICommandSender       _sender;

  /**
   * A reference to the ClientCommandManager instance.
   */
  protected ClientCommandManager _ccm;

} // class ClientCommandSender