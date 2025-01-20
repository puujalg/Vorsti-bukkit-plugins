package vorst.treecapitator;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TreeHandler implements Listener {
    private final List<Material> logMaterials = Arrays.asList(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.ACACIA_LOG,
            Material.CHERRY_LOG, Material.DARK_OAK_LOG, Material.JUNGLE_LOG,
            Material.MANGROVE_LOG, Material.SPRUCE_LOG, Material.PALE_OAK_LOG
    );

    private final List<Material> axeMaterials = Arrays.asList(
            Material.WOODEN_AXE, Material.STONE_AXE,
            Material.IRON_AXE, Material.GOLDEN_AXE,
            Material.DIAMOND_AXE, Material.NETHERITE_AXE
    );

    public TreeHandler() {
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!e.isCancelled() && e.getPlayer().getGameMode().equals(org.bukkit.GameMode.SURVIVAL)) {
            Material blockType = e.getBlock().getType();

            if (isLogMaterial(blockType)) {
                Block block = e.getBlock();
                boolean isComplicated = isComplicatedTree(block);

                Material handItemType = e.getPlayer().getInventory().getItemInMainHand().getType();
                if (isAxeMaterial(handItemType)) {
                    if (isComplicated) {
                        breakTree(block, e.getPlayer());
                    } else {
                        breakSimpleTree(block, e.getPlayer());
                    }
                }
            }
        }
    }

    public void breakTree(Block block, Player player) {
        Queue<Block> blocksToBreak = new LinkedList<>();
        blocksToBreak.add(block);

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        while (!blocksToBreak.isEmpty()) {
            Block currentBlock = blocksToBreak.poll();
            currentBlock.breakNaturally();

            // Check all 26 possible close positions (including diagonals)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block adjacentBlock = currentBlock.getRelative(dx, dy, dz);
                        if (isLogMaterial(adjacentBlock.getType())) {
                            blocksToBreak.add(adjacentBlock);
                        }
                    }
                }
            }
            if (handleToolDurability(mainHandItem, player)) {
                break; // tool broken, gtfo
            }
        }
    }

    public void breakSimpleTree(Block block, Player player) {
        Block currentBlock = block;
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        while (isLogMaterial(currentBlock.getType())) {
            currentBlock.breakNaturally();

            if (handleToolDurability(mainHandItem, player)) {
                break; // broken tool
            }
            currentBlock = currentBlock.getRelative(BlockFace.UP);
        }
    }

    public boolean isComplicatedTree(Block logBlock) {
        Block currentBlock = logBlock;

        while (isLogMaterial(currentBlock.getType())) {
            for (BlockFace face : new BlockFace[]{
                    BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
                    BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST,
                    BlockFace.UP, BlockFace.DOWN
            }) {
                Block adjacentBlock = currentBlock.getRelative(face);

                if (isLogMaterial(adjacentBlock.getType())) {
                    if (!isPartOfSimpleTrunk(logBlock, adjacentBlock)) {
                        return true;
                    }
                }
            }
            currentBlock = currentBlock.getRelative(BlockFace.UP);
        }
        return false;
    }

    private boolean handleToolDurability(ItemStack mainHandItem, Player player) {
        ItemMeta itemMeta = mainHandItem.getItemMeta();

        if (!(itemMeta instanceof Damageable)) {
            return false;
        }

        Damageable damageableItem = (Damageable) itemMeta;
        int newDamage = damageableItem.getDamage() + 1;

        // check if tool is broken
        if (newDamage >= mainHandItem.getType().getMaxDurability()) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR)); // remove item from inventory
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
            return true; // tool is broken
        } else {
            damageableItem.setDamage(newDamage);
            mainHandItem.setItemMeta(itemMeta); // update the item with new damage value
            return false; // tool not broken
        }
    }

    private boolean isPartOfSimpleTrunk(Block baseLog, Block checkLog) {
        return checkLog.getRelative(BlockFace.UP).equals(baseLog) || checkLog.getRelative(BlockFace.DOWN).equals(baseLog);
    }

    private boolean isLogMaterial(Material material) {
        return logMaterials.contains(material);
    }

    private boolean isAxeMaterial(Material material) {
        return axeMaterials.contains(material);
    }
}
