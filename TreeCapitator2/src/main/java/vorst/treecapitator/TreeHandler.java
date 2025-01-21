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

import java.util.*;

public class TreeHandler implements Listener {
    private final List<Material> logMaterials = Arrays.asList(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.ACACIA_LOG,
            Material.CHERRY_LOG, Material.DARK_OAK_LOG, Material.JUNGLE_LOG,
            Material.MANGROVE_LOG, Material.SPRUCE_LOG, Material.PALE_OAK_LOG
    );

    private final List<Material> leafMaterials = Arrays.asList(
            Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.ACACIA_LEAVES,
            Material.CHERRY_LEAVES, Material.DARK_OAK_LEAVES, Material.JUNGLE_LEAVES,
            Material.MANGROVE_LEAVES, Material.SPRUCE_LEAVES, Material.PALE_OAK_LEAVES,
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
        Set<Block> brokenBlocks = new HashSet<>(); // count blocks
        int blocksBroken = 0;

        while (!blocksToBreak.isEmpty()) {
            Block currentBlock = blocksToBreak.poll();
            if (brokenBlocks.contains(currentBlock)) {
                continue; // no need to continue if already borken... maybe
            }

            currentBlock.breakNaturally();
            brokenBlocks.add(currentBlock); // add blocks idk
            blocksBroken++; // count blocks

            // check near blocks.. 26? smth
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue; // skip current block
                        Block adjacentBlock = currentBlock.getRelative(dx, dy, dz);
                        if (isLogMaterial(adjacentBlock.getType()) && !brokenBlocks.contains(adjacentBlock)) {
                            blocksToBreak.add(adjacentBlock);
                        }
                    }
                }
            }
        }

        // After breaking all logs, apply durability damage
        if (handleToolDurability(mainHandItem, player, blocksBroken)) {
            return; // questionable... can refractor prolly
        }
    }

    public void breakSimpleTree(Block block, Player player) {
        Block currentBlock = block;
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        Set<Block> brokenBlocks = new HashSet<>(); // how many block borken?
        int blocksBroken = 0;

        while (isLogMaterial(currentBlock.getType())) {
            if (brokenBlocks.contains(currentBlock)) {
                break; // tool broken, exit
            }

            currentBlock.breakNaturally();
            brokenBlocks.add(currentBlock);
            blocksBroken++;

            // next block vertical
            currentBlock = currentBlock.getRelative(BlockFace.UP);
        }

        // after breaking, add damage
        if (handleToolDurability(mainHandItem, player, blocksBroken)) {
            return; // questionable... can refractor prolly (cuz void)
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

    private boolean handleToolDurability(ItemStack mainHandItem, Player player, int blocksBroken) {
        ItemMeta itemMeta = mainHandItem.getItemMeta();

        if (!(itemMeta instanceof Damageable)) {
            return false;
        }

        Damageable damageableItem = (Damageable) itemMeta;
        int newDamage = damageableItem.getDamage() + blocksBroken;

        // tool borken check
        if (newDamage >= mainHandItem.getType().getMaxDurability()) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR)); // remove tool
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
            return true; // tool is poof
        } else {
            damageableItem.setDamage(newDamage);
            mainHandItem.setItemMeta(itemMeta); // new damage
            return false; // tool ok
        }
    }

    public boolean isTree(Block logBlock) {
        Block currentBlock = logBlock;

        // check for leaves
        do {
            if (hasLeavesNextToLog(currentBlock)) {
                return true;
            }
            currentBlock = currentBlock.getRelative(BlockFace.UP);  // upward check
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

    private boolean isPartOfSimpleTrunk(Block baseLog, Block checkLog) {
        return checkLog.getRelative(BlockFace.UP).equals(baseLog) || checkLog.getRelative(BlockFace.DOWN).equals(baseLog);
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
