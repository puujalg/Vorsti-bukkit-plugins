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
import java.util.List;

public class TreeHandler implements Listener {
    private final List<Material> logMaterials = Arrays.asList(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.ACACIA_LOG,
            Material.CHERRY_LOG, Material.DARK_OAK_LOG, Material.JUNGLE_LOG,
            Material.MANGROVE_LOG, Material.SPRUCE_LOG
    );

    private final List<Material> leafMaterials = Arrays.asList(
            Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.ACACIA_LEAVES,
            Material.CHERRY_LEAVES, Material.DARK_OAK_LEAVES, Material.JUNGLE_LEAVES,
            Material.MANGROVE_LEAVES, Material.SPRUCE_LEAVES,
            Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES
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

            if (isLogMaterial(blockType) && isTree(e.getBlock())) {
                Material handItemType = e.getPlayer().getInventory().getItemInMainHand().getType();

                if (isAxeMaterial(handItemType)) {
                    breakTree(e.getBlock(), e.getPlayer());
                }
            }
        }
    }

    public void breakTree(Block block, Player player) {

        block.breakNaturally();
        Block blockAbove = block.getRelative(BlockFace.UP);

        if (isLogMaterial(blockAbove.getType())) {
            breakTree(blockAbove, player);

            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            ItemMeta itemMeta = mainHandItem.getItemMeta();

            if (itemMeta instanceof Damageable) {
                Damageable damageableItem = (Damageable) itemMeta;
                int newDamage = damageableItem.getDamage() + 1;

                Material material = mainHandItem.getType();
                int maxDurability = material.getMaxDurability();

                if (newDamage >= maxDurability) {
                    player.getInventory().removeItem(mainHandItem);
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_METAL_BREAK, 1.0F, 1.0F);
                } else {
                    ItemMeta updatedMeta = itemMeta.clone();
                    ((Damageable) updatedMeta).setDamage(newDamage);
                    mainHandItem.setItemMeta(updatedMeta);
                    player.getInventory().setItemInMainHand(mainHandItem);
                }
            }
        }
    }

    public boolean isTree(Block logBlock) {
        Block currentBlock = logBlock;

        do {
            if (hasLeavesNextToLog(currentBlock)) {
                return true;
            }
            currentBlock = currentBlock.getRelative(BlockFace.UP);
        } while (isLogMaterial(currentBlock.getType()));
        return false;
    }

    public boolean hasLeavesNextToLog(Block logBlock) {
        for (BlockFace face : BlockFace.values()) {
            Block adjacentBlock = logBlock.getRelative(face);
            if (isLeafMaterial(adjacentBlock.getType())) {
                return true;
            }
        }
        return false;
    }

    private boolean isLogMaterial(Material material) {
        return logMaterials.contains(material);
    }

    private boolean isLeafMaterial(Material material) {
        return leafMaterials.contains(material);
    }

    private boolean isAxeMaterial(Material material) {
        return axeMaterials.contains(material);
    }
}