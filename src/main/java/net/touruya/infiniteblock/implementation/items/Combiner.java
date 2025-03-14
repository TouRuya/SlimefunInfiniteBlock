package net.touruya.infiniteblock.implementation.items;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import net.touruya.infiniteblock.api.stored.Stored;
import net.touruya.infiniteblock.core.commands.StorageCommand;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class Combiner extends AContainer {
    private static final ItemStack BACKGROUND = new CustomItemStack(Material.GRAY_STAINED_GLASS_PANE, " ", " ");
    private static final int[] BACKGROUND_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8,
            45, 46, 47, 48, 50, 51, 52, 53,
    };

    private static final int[] INPUT_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44,
    };

    private static final int PROGRESS_SLOT = 4;
    private static final int OUTPUT_SLOT = 49;

    public Combiner(@NotNull ItemGroup category, @NotNull SlimefunItemStack item, @NotNull RecipeType recipeType, ItemStack @NotNull [] recipe) {
        super(category, item, recipeType, recipe);
        new BlockMenuPreset(this.getId(), getItemName()) {

            @Override
            public void init() {
                for (int i : BACKGROUND_SLOTS) {
                    addItem(i, BACKGROUND, ChestMenuUtils.getEmptyClickHandler());
                }
                addItem(PROGRESS_SLOT, BACKGROUND);
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return player.hasPermission("slimefun.inventory.bypass") || Slimefun.getProtectionManager().hasPermission(player, block, Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow itemTransportFlow) {
                if (itemTransportFlow == ItemTransportFlow.INSERT) {
                    return INPUT_SLOTS;
                } else {
                    return new int[]{OUTPUT_SLOT};
                }
            }
        };
    }

    @Override
    public void tick(Block block) {
        BlockMenu menu = StorageCacheUtils.getMenu(block.getLocation());
        if (menu == null) {
            return;
        }

        craft(menu);
    }

    @Override
    public ItemStack getProgressBar() {
        return BACKGROUND;
    }

    @Override
    public @NotNull String getMachineIdentifier() {
        return getId();
    }

    public static boolean isCombinedBlock(ItemStack itemStack) {
        return SlimefunItem.getByItem(itemStack) instanceof CombinedBlock;
    }

    public static @NotNull ItemStack getUnpackedItem(@NotNull ItemStack combined) {
        Stored stored = CombinedBlock.getStoredFromCombined(combined);
        if (stored == null) {
            return new ItemStack(Material.AIR);
        }
        long amount = CombinedBlock.getStoredAmountFromCombined(combined);
        return new CustomItemStack(stored.getItemStack(), (int) amount);
    }

    @CanIgnoreReturnValue
    public boolean craft(@NotNull BlockMenu menu) {
        if (!this.takeCharge(menu.getLocation())) {
            feedback(menu, "电力不足", false);
            return false;
        }

        ItemStack innerItem = null;
        long totalAmount = 0;
        boolean isAllSimilar = true;
        for (int inputSlot : INPUT_SLOTS) {
            ItemStack itemStack = menu.getItemInSlot(inputSlot);
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }

            int combinedAmount = 1;
            if (isCombinedBlock(itemStack)) {
                combinedAmount = itemStack.getAmount();
                itemStack = getUnpackedItem(itemStack);
            }

            if (innerItem == null) {
                if (!isCombinedBlock(itemStack)) {
                    innerItem = itemStack;
                } else {
                    innerItem = getUnpackedItem(itemStack);
                }
            } else {
                if (!SlimefunUtils.isItemSimilar(innerItem, itemStack, true, false)) {
                    isAllSimilar = false;
                    break;
                }
            }

            totalAmount += (long) itemStack.getAmount() * combinedAmount;
        }

        if (innerItem == null) {
            feedback(menu, "请确保至少有一个输入物品", false);
            return false;
        }

        if (!isAllSimilar) {
            feedback(menu, "请确保所有输入物品都相同", false);
            return false;
        }

        if (innerItem.getType() == Material.AIR || !innerItem.getType().isBlock()) {
            feedback(menu, "请确保输入物品为方块", false);
            return false;
        }

        if (totalAmount <= 0) {
            feedback(menu, "请确保输入物品数量大于0", false);
            return false;
        }

        final ItemStack exisitingOutput = menu.getItemInSlot(OUTPUT_SLOT);
        if (exisitingOutput != null && exisitingOutput.getType() != Material.AIR) {
            feedback(menu, "输出槽已有物品", false);
            return false;
        }

        // consume items
        for (final int inputSlot : INPUT_SLOTS) {
            menu.replaceExistingItem(inputSlot, new ItemStack(Material.AIR));
        }

        if (totalAmount > Integer.MAX_VALUE) {
            feedback(menu, "输入物品数量过多", false);
            return false;
        }

        // push item
        final ItemStack itemStack = StorageCommand.create(innerItem, totalAmount);

        menu.pushItem(itemStack, OUTPUT_SLOT);
        feedback(menu, "工作中", true);

        return true;
    }

    public static void feedback(@NotNull BlockMenu menu, @NotNull String message, boolean success) {
        menu.replaceExistingItem(PROGRESS_SLOT, new CustomItemStack(
                success? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                " ",
                (success? "&a" : "&c") + message
        ));
    }

    @Override
    public int getCapacity() {
        return 8192;
    }

    @Override
    public int getEnergyConsumption() {
        return 128;
    }

    @Override
    public int getSpeed() {
        return 1;
    }
}
