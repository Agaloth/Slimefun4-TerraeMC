package io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks;

import java.util.List;
import java.util.UUID;

import io.github.thebusybiscuit.slimefun4.utils.PatternUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.cscorelib2.item.CustomItem;
import io.github.thebusybiscuit.slimefun4.implementation.items.tools.SlimefunBackpack;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.BackpackListener;
import me.mrCookieSlime.Slimefun.SlimefunPlugin;
import me.mrCookieSlime.Slimefun.Lists.Categories;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Lists.SlimefunItems;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.multiblocks.MultiBlockMachine;
import me.mrCookieSlime.Slimefun.Setup.SlimefunManager;
import me.mrCookieSlime.Slimefun.api.PlayerProfile;
import me.mrCookieSlime.Slimefun.api.Slimefun;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;

public class MagicWorkbench extends MultiBlockMachine {

	public MagicWorkbench() {
		super(
				Categories.MACHINES_1, 
				(SlimefunItemStack) SlimefunItems.MAGIC_WORKBENCH, 
				new ItemStack[] {null, null, null, null, null, null, new ItemStack(Material.BOOKSHELF), new ItemStack(Material.CRAFTING_TABLE), new ItemStack(Material.DISPENSER)},
				new ItemStack[0], 
				BlockFace.UP
		);
	}
	
	@Override
	public void onInteract(Player p, Block b) {
		Block dispBlock = null;

		// Maybe this could be implemented by instead looping over a BlockFace<> array?
		if (b.getRelative(1, 0, 0).getType() == Material.DISPENSER) dispBlock = b.getRelative(1, 0, 0);
		else if (b.getRelative(0, 0, 1).getType() == Material.DISPENSER) dispBlock = b.getRelative(0, 0, 1);
		else if (b.getRelative(-1, 0, 0).getType() == Material.DISPENSER) dispBlock = b.getRelative(-1, 0, 0);
		else if (b.getRelative(0, 0, -1).getType() == Material.DISPENSER) dispBlock = b.getRelative(0, 0, -1);

		Dispenser disp = (Dispenser) dispBlock.getState();
		Inventory inv = disp.getInventory();
		List<ItemStack[]> inputs = RecipeType.getRecipeInputList(this);

		for (int i = 0; i < inputs.size(); i++) {
			if (isCraftable(inv, inputs.get(i))) {
				ItemStack adding = RecipeType.getRecipeOutputList(this, inputs.get(i)).clone();
				
				if (Slimefun.hasUnlocked(p, adding, true)) {
					Inventory inv2 = Bukkit.createInventory(null, 9, "test");

					for (int j = 0; j < inv.getContents().length; j++) {
						inv2.setItem(j, inv.getContents()[j] != null ? (inv.getContents()[j].getAmount() > 1 ? new CustomItem(inv.getContents()[j], inv.getContents()[j].getAmount() - 1): null): null);
					}

					Inventory outputInv = findOutputInventory(adding, dispBlock, inv, inv2);
					if (outputInv != null) {
						SlimefunItem sfItem = SlimefunItem.getByItem(adding);

						if (sfItem instanceof SlimefunBackpack) {
							ItemStack backpack = null;

							for (int j = 0; j < 9; j++) {
								if (inv.getContents()[j] != null && inv.getContents()[j].getType() != Material.AIR && SlimefunItem.getByItem(inv.getContents()[j]) instanceof SlimefunBackpack) {
									backpack = inv.getContents()[j];
									break;
								}
							}

							String id = "";
							int size = ((SlimefunBackpack) sfItem).getSize();

							if (backpack != null) {
								for (String line : backpack.getItemMeta().getLore()) {
									if (line.startsWith(ChatColor.translateAlternateColorCodes('&', "&7ID: ")) && line.contains("#")) {
										id = line.replace(ChatColor.translateAlternateColorCodes('&', "&7ID: "), "");
										String[] idSplit = PatternUtils.HASH.split(id);
										PlayerProfile.fromUUID(UUID.fromString(idSplit[0])).getBackpack(Integer.parseInt(idSplit[1])).setSize(size);
										break;
									}
								}
							}

							if (id.isEmpty()) {
								for (int line = 0; line < adding.getItemMeta().getLore().size(); line++) {
									if (adding.getItemMeta().getLore().get(line).equals(ChatColor.translateAlternateColorCodes('&', "&7ID: <ID>"))) {
										int backpackID = PlayerProfile.get(p).createBackpack(size).getID();

                                        BackpackListener.setBackpackId(p, adding, line, backpackID);
                                    }
								}
							}
							else {
								for (int line = 0; line < adding.getItemMeta().getLore().size(); line++) {
									if (adding.getItemMeta().getLore().get(line).equals(ChatColor.translateAlternateColorCodes('&', "&7ID: <ID>"))) {
										ItemMeta im = adding.getItemMeta();
										List<String> lore = im.getLore();
										lore.set(line, lore.get(line).replace("<ID>", id));
										im.setLore(lore);
										adding.setItemMeta(im);
										break;
									}
								}
							}
						}

						for (int j = 0; j < 9; j++) {
							if (inv.getContents()[j] != null && inv.getContents()[j].getType() != Material.AIR) {
								if (inv.getContents()[j].getAmount() > 1) inv.setItem(j, new CustomItem(inv.getContents()[j], inv.getContents()[j].getAmount() - 1));
								else inv.setItem(j, null);
							}
						}
						
						for (int j = 0; j < 4; j++) {
							int current = j;
							Bukkit.getScheduler().runTaskLater(SlimefunPlugin.instance, () -> {
								p.getWorld().playEffect(b.getLocation(), Effect.MOBSPAWNER_FLAMES, 1);
								p.getWorld().playEffect(b.getLocation(), Effect.ENDER_SIGNAL, 1);
								
								if (current < 3) {
									p.getWorld().playSound(b.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
								} 
								else {
									p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 1F);
									outputInv.addItem(adding);
								}
							}, j*20L);
						}
					}
					else SlimefunPlugin.getLocal().sendMessage(p, "machines.full-inventory", true);
				}
				
				return;
			}
		}
		SlimefunPlugin.getLocal().sendMessage(p, "machines.pattern-not-found", true);
	}

	private boolean isCraftable(Inventory inv, ItemStack[] recipe) {
		for (int j = 0; j < inv.getContents().length; j++) {
			if (!SlimefunManager.isItemSimilar(inv.getContents()[j], recipe[j], true)) {
				if (SlimefunItem.getByItem(recipe[j]) instanceof SlimefunBackpack) {
					if (!SlimefunManager.isItemSimilar(inv.getContents()[j], recipe[j], false)) {
						return false;
					}
				}
				else {
					return false;
				}
			}
		}
		
		return true;
	}

}
