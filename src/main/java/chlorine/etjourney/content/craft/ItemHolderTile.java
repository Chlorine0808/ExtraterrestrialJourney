package chlorine.etjourney.content.craft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/** A device surface that holds a few single items, placed and taken by hand, and shows them to every client. */
public class ItemHolderTile extends TileEntity {

    private final int capacity;
    private final List<ItemStack> items = new ArrayList<>();

    ItemHolderTile(int capacity) {
        this.capacity = capacity;
    }

    List<ItemStack> items() {
        return Collections.unmodifiableList(items);
    }

    /** Takes one item off the held stack; false when the holder is full. */
    boolean place(ItemStack held) {
        if (items.size() >= capacity) return false;
        items.add(held.splitStack(1));
        changed();
        return true;
    }

    /** The last item placed, or null when empty. */
    ItemStack takeLast() {
        if (items.isEmpty()) return null;
        ItemStack taken = items.remove(items.size() - 1);
        changed();
        return taken;
    }

    void clear() {
        items.clear();
        changed();
    }

    void replaceAll(ItemStack stack) {
        items.clear();
        items.add(stack);
        changed();
    }

    void dropAll() {
        for (ItemStack stack : items) spawn(stack);
        items.clear();
    }

    void spawn(ItemStack stack) {
        EntityItem entity = new EntityItem(worldObj, xCoord + 0.5, yCoord + 1.1, zCoord + 0.5, stack);
        entity.motionX = entity.motionY = entity.motionZ = 0;
        worldObj.spawnEntityInWorld(entity);
    }

    private void changed() {
        markDirty();
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeItems(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readItems(tag);
    }

    private void writeItems(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : items) list.appendTag(stack.writeToNBT(new NBTTagCompound()));
        tag.setTag("Items", list);
    }

    private void readItems(NBTTagCompound tag) {
        items.clear();
        NBTTagList list = tag.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount() && i < capacity; i++) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
            if (stack != null) items.add(stack);
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeItems(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        readItems(packet.func_148857_g());
    }
}
