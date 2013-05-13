package net.mcft.copy.betterstorage.addon.thaumcraft;

import thaumcraft.api.EnumTag;
import thaumcraft.api.IVisDiscounter;
import thaumcraft.api.IVisRepairable;
import thaumcraft.api.ObjectTags;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aura.AuraNode;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mcft.copy.betterstorage.item.ItemBackpack;
import net.mcft.copy.betterstorage.utils.RandomUtils;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemThaumcraftBackpack extends ItemBackpack implements IVisRepairable, IVisDiscounter {
	
	public ItemThaumcraftBackpack(int id) {
		super(id, material);
		setMaxDamage(290);
	}
	
	@Override
	public String getName() { return "container.thaumcraftBackpack"; }
	
	@Override
	public int getColumns() { return 13; }
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister iconRegister) {
		itemIcon = iconRegister.registerIcon("betterstorage:thaumcraftBackpack");
	}
	
	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, int slot, int layer) {
		return ThaumcraftAddon.thaumcraftBackpackTexture;
	}
	
	@Override
	public int getItemEnchantability() { return 25; }
	
	@Override
	public void onArmorTickUpdate(World world, EntityPlayer player, ItemStack itemStack) {
		super.onArmorTickUpdate(world, player, itemStack);
		if (player.worldObj.isRemote || (itemStack.stackSize == 0)) return;
		createFlux(player, itemStack);
		fluxEffects(player, itemStack);
		repairItems(player, itemStack);
	}
	
	private void createFlux(EntityPlayer player, ItemStack backpack) {
		
		// Every 5 seconds.
		if (player.ticksExisted % 100 != 0) return;
		
		// Count items over normal backpack capacity.
		IInventory inventory = ItemBackpack.getBackpackItems(player);
		int count = -(getRows() * 9);
		for (int i = 0; i < inventory.getSizeInventory(); i++)
			if (inventory.getStackInSlot(i) != null) count++;
		
		// When count <= 0, return.
		// When count = 1, chance is ~4%.
		// When count = 12, chance is 50%.
		if (RandomUtils.getInt(24) > count) return;
		
		int auraId = ThaumcraftApi.getClosestAuraWithinRange(player.worldObj, player.posX, player.posY, player.posZ, 640);
		if (auraId == -1) return;
		ThaumcraftApi.queueNodeChanges(auraId, 0, 0, false, (new ObjectTags()).add(EnumTag.VOID, 1), 0, 0, 0);
		
	}
	
	private void fluxEffects(EntityPlayer player, ItemStack backpack) {
		
		// Every 10 seconds.
		if (player.ticksExisted % 200 != 0) return;
		
		// Get closest aura node.
		int auraId = ThaumcraftApi.getClosestAuraWithinRange(player.worldObj, player.posX, player.posY, player.posZ, 640);
		if (auraId == -1) return;
		AuraNode aura = ThaumcraftApi.getNodeCopy(auraId);
		
		// Get and count flux.
		EnumTag[] aspects = aura.flux.getAspectsSortedAmount();
		int total = 0;
		for (int i = 0; i < aspects.length; i++)
			total += aura.flux.tags.get(aspects[i]);
		
		// The higher the flux is, the bigger the chance for a random effect.
		if (RandomUtils.getInt(64, 512) > total) return;
		
		ObjectTags fluxReduce = new ObjectTags();
		BackpackFluxEffect effect = null;
		
		// Go through all flux aspects in the aura, from highest to lowest.
		for (int i = 0; i < aspects.length; i++) {
			EnumTag aspect = aspects[i];
			int amount = aura.flux.tags.get(aspect);
			if (RandomUtils.getInt(10, 48) < amount) {
				// If that flux from that aspect is high enough,
				// use the specific effect for it.
				effect = BackpackFluxEffect.effects.get(aspect);
				if (effect != null) {
					fluxReduce = (new ObjectTags()).add(aspect, -10);
					break;
				}
			}
			fluxReduce.add(aspect, -1);
			if (fluxReduce.tags.size() >= 10) break;
		}
		
		// If we don't have an effect yet, pick a random one.
		if (effect == null) {
			int index = RandomUtils.getInt(BackpackFluxEffect.effects.size()), i = 0;
			for (BackpackFluxEffect e : BackpackFluxEffect.effects.values())
				if (i++ == index) effect = e;
		}
		effect.apply(player, backpack);
		
		// Remove some flux from the aura.
		ThaumcraftApi.queueNodeChanges(auraId, 0, 0, false, fluxReduce, 0, 0, 0);
		
	}
	
	private void repairItems(EntityPlayer player, ItemStack backpack) {
		
		// TODO
		
	}
	
	// Thaumcraft implementations
	
	@Override
	public void doRepair(ItemStack stack, Entity e) {
		if (ThaumcraftApi.decreaseClosestAura(e.worldObj, e.posX, e.posY, e.posZ, 1, true))
			stack.damageItem(-1, (EntityLiving)e);
	}
	@Override
	public int getVisDiscount() { return 5; }
	
}
