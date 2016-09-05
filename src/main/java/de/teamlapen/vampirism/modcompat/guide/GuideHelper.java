package de.teamlapen.vampirism.modcompat.guide;

import amerifrance.guideapi.api.IPage;
import amerifrance.guideapi.api.IRecipeRenderer;
import amerifrance.guideapi.api.impl.abstraction.EntryAbstract;
import amerifrance.guideapi.api.util.PageHelper;
import amerifrance.guideapi.page.PageFurnaceRecipe;
import amerifrance.guideapi.page.PageIRecipe;
import amerifrance.guideapi.page.PageItemStack;
import amerifrance.guideapi.page.PageText;
import com.google.common.collect.Lists;
import de.teamlapen.lib.lib.util.UtilLib;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.api.items.IHunterWeaponRecipe;
import de.teamlapen.vampirism.api.items.IItemWithTier;
import de.teamlapen.vampirism.core.ModItems;
import de.teamlapen.vampirism.inventory.HunterWeaponCraftingManager;
import de.teamlapen.vampirism.inventory.ShapedHunterWeaponRecipe;
import de.teamlapen.vampirism.inventory.ShapelessHunterWeaponRecipe;
import de.teamlapen.vampirism.modcompat.guide.pages.PageHolderWithLinks;
import de.teamlapen.vampirism.modcompat.guide.pages.ShapedWeaponTableRecipeRenderer;
import de.teamlapen.vampirism.modcompat.guide.pages.ShapelessWeaponTableRecipeRenderer;
import joptsimple.internal.Strings;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.text.WordUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Collection of helper methods
 */
public class GuideHelper {

    /**
     * Adds multiple strings together seperated by a double line break
     *
     * @param unlocalized Unlocalized strings
     */
    public static String append(String... unlocalized) {
        String s = "";
        for (String u : unlocalized) {
            s += UtilLib.translate(u) + "\n\n";
        }
        return s;
    }

    /**
     * Converts the given pages to {@link PageHolderWithLinks} and adds the given links
     * @return The SAME list
     */
    public static List<IPage> addLinks(List<IPage> pages, Object... links) {
        List<PageHolderWithLinks> linkPages = Lists.newArrayList();
        for (IPage p : pages) {
            linkPages.add(new PageHolderWithLinks(p));
        }

        for (Object l : links) {
            if (l instanceof ResourceLocation) {
                for (PageHolderWithLinks p : linkPages) {
                    p.addLink((ResourceLocation) l);
                }
            } else if (l instanceof EntryAbstract) {
                for (PageHolderWithLinks p : linkPages) {
                    p.addLink((EntryAbstract) l);
                }
            } else if (l instanceof PageHolderWithLinks.URLLink) {
                for (PageHolderWithLinks p : linkPages) {
                    p.addLink((PageHolderWithLinks.URLLink) l);
                }
            } else {
                VampirismMod.log.w("GuideHelper", "Given link object cannot be linked %s", l);
            }
        }
        pages.clear();
        pages.addAll(linkPages);
        return pages;
    }

    public static IRecipe getRecipeForOutput(ItemStack stack) {
        for (IRecipe recipe : CraftingManager.getInstance().getRecipeList()) {
            if (checkRecipeOutput(recipe, stack, true)) return recipe;
        }

        return null;
    }

    public static IHunterWeaponRecipe getWeaponTableRecipeForOutput(ItemStack stack) {
        for (IHunterWeaponRecipe recipe : HunterWeaponCraftingManager.getInstance().getRecipes()) {
            if (checkRecipeOutput(recipe, stack, true)) return recipe;
        }
        return null;
    }

    public static ItemStack getFurnaceRecipe(ItemStack stack) {
        for (Map.Entry<ItemStack, ItemStack> e : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
            if (stack.isItemEqual(e.getValue())) {
                return e.getKey();
            }
        }
        return null;
    }

    private static boolean checkRecipeOutput(IRecipe recipe, ItemStack stack, boolean checkNBT) {
        if (recipe != null) {
            ItemStack resultStack = recipe.getRecipeOutput();
            if (resultStack != null && resultStack.getItem() != null) {
                if (resultStack.getItem() == stack.getItem() && resultStack.getItemDamage() == stack.getItemDamage()) {
                    if (!checkNBT || resultStack.getTagCompound() == null && stack.getTagCompound() == null || resultStack.getTagCompound() != null && resultStack.getTagCompound().equals(stack.getTagCompound())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static
    @Nullable
    IPage getRecipePage(ItemStack stack, RECIPE_TYPE type) {
        try {
            switch (type) {
                case WORKBENCH:
                    IRecipe r = checkNotNull(getRecipeForOutput(stack));
                    return new PageIRecipe(r);
                case FURNACE:
                    ItemStack s = checkNotNull(getFurnaceRecipe(stack));
                    return new PageFurnaceRecipe(s);
                case WEAPON_TABLE:
                    IHunterWeaponRecipe r2 = checkNotNull(getWeaponTableRecipeForOutput(stack));
                    IRecipeRenderer renderer = null;
                    if (r2 instanceof ShapedHunterWeaponRecipe) {
                        renderer = new ShapedWeaponTableRecipeRenderer((ShapedHunterWeaponRecipe) r2);
                    } else if (r2 instanceof ShapelessHunterWeaponRecipe) {
                        renderer = new ShapelessWeaponTableRecipeRenderer((ShapelessHunterWeaponRecipe) r2);
                    }
                    checkNotNull(renderer);
                    return new PageIRecipe(r2, renderer);
                default:
                    throw new IllegalArgumentException("Type unknown " + type);
            }
        } catch (NullPointerException e) {
            return null;
        }
    }

    public static void addArmorWithTier(Map<ResourceLocation, EntryAbstract> entries, String name, IItemWithTier helmet, IItemWithTier chest, IItemWithTier legs, IItemWithTier boots, RECIPE_TYPE recipeType) {
        List<ItemStack> items = new ArrayList<>();
        for (IItemWithTier.TIER t : IItemWithTier.TIER.values()) {
            items.add(ModItems.createStack(helmet, t));
            items.add(ModItems.createStack(chest, t));
            items.add(ModItems.createStack(legs, t));
            items.add(ModItems.createStack(boots, t));
        }
        ItemInfoBuilder builder = new ItemInfoBuilder(ModItems.createStack(helmet, IItemWithTier.TIER.NORMAL), false);
        builder.setName(name).ignoreMissingRecipes().craftable(RECIPE_TYPE.WEAPON_TABLE).craftableStacks(items).customName();
        builder.build(entries);
    }

    public static void addItemWithTier(Map<ResourceLocation, EntryAbstract> entries, IItemWithTier item, RECIPE_TYPE recipeType) {
        List<ItemStack> items = new ArrayList<>();
        for (IItemWithTier.TIER t : IItemWithTier.TIER.values()) {
            items.add(ModItems.createStack(item, t));
        }
        ItemInfoBuilder builder = new ItemInfoBuilder(ModItems.createStack(item, IItemWithTier.TIER.NORMAL), false);
        builder.craftable(RECIPE_TYPE.WEAPON_TABLE).craftableStacks(items).ignoreMissingRecipes();
        builder.build(entries);
    }

    public static List<IPage> pagesForLongText(String locText, ItemStack stack, int length) {
        List<IPage> pageList = new ArrayList<IPage>();
        String[] strings = WordUtils.wrap(locText, Math.max(length - 150, 50), "/cut", false).split("/cut");
        pageList.add(new PageItemStack(strings[0], stack));
        if (strings.length > 2) {
            String s = Strings.join(Arrays.copyOfRange(strings, 1, strings.length), "");
            pageList.addAll(PageHelper.pagesForLongText(s, length));
        } else if (strings.length == 2) {
            pageList.add(new PageText(strings[1]));
        }
        return pageList;
    }

    public static List<IPage> pagesForLongText(String locText, ItemStack stack) {
        return pagesForLongText(locText, stack, 290);
    }

    public enum RECIPE_TYPE {
        WORKBENCH, FURNACE, WEAPON_TABLE
    }


}