package blusunrize.immersiveengineering.common.crafting;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

public class RecipePowerpack implements IRecipe
{
	@Override
	public boolean matches(InventoryCrafting inv, World world)
	{
		ItemStack powerpack = ItemStack.EMPTY;
		ItemStack armor = ItemStack.EMPTY;
		for(int i=0;i<inv.getSizeInventory();i++)
		{
			ItemStack stackInSlot = inv.getStackInSlot(i);
			if(!stackInSlot.isEmpty())
				if(powerpack.isEmpty() && IEContent.itemPowerpack.equals(stackInSlot.getItem()))
					powerpack = stackInSlot;
				else if(armor.isEmpty() && stackInSlot.getItem() instanceof ItemArmor && ((ItemArmor) stackInSlot.getItem()).armorType == EntityEquipmentSlot.CHEST && !ImmersiveEngineering.proxy.armorHasCustomModel(stackInSlot))
					armor = stackInSlot;
				else
					return false;
		}
		if(!powerpack.isEmpty() && !armor.isEmpty())
			return true;
		else if(!armor.isEmpty() && ItemNBTHelper.hasKey(armor, Lib.NBT_Powerpack) && powerpack.isEmpty())
			return true;
		return false;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv)
	{
		ItemStack powerpack = ItemStack.EMPTY;
		ItemStack armor = ItemStack.EMPTY;
		for(int i=0;i<inv.getSizeInventory();i++)
		{
			ItemStack stackInSlot = inv.getStackInSlot(i);
			if(!stackInSlot.isEmpty())
				if(powerpack.isEmpty() && IEContent.itemPowerpack.equals(stackInSlot.getItem()))
					powerpack = stackInSlot;
				else if(armor.isEmpty() && stackInSlot.getItem() instanceof ItemArmor && ((ItemArmor)stackInSlot.getItem()).armorType==EntityEquipmentSlot.CHEST && !ImmersiveEngineering.proxy.armorHasCustomModel(stackInSlot))
					armor = stackInSlot;
		}

		if(!powerpack.isEmpty() && !armor.isEmpty())
		{
			ItemStack output = armor.copy();
			ItemNBTHelper.setItemStack(output, Lib.NBT_Powerpack, powerpack.copy());

			return output;
		}
		else if(!armor.isEmpty() && ItemNBTHelper.hasKey(armor, Lib.NBT_Powerpack))
		{
			ItemStack output = armor.copy();
			ItemNBTHelper.remove(output, Lib.NBT_Powerpack);
			return output;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public int getRecipeSize()
	{
		return 10;
	}
	@Override
	public ItemStack getRecipeOutput()
	{
		return new ItemStack(IEContent.itemPowerpack,1,0);
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv)
	{
		NonNullList<ItemStack> remaining = ForgeHooks.defaultRecipeGetRemainingItems(inv);
		for(int i=0;i<remaining.size();i++)
		{
			ItemStack stackInSlot = inv.getStackInSlot(i);
			if(!stackInSlot.isEmpty() && ItemNBTHelper.hasKey(stackInSlot, Lib.NBT_Powerpack))
				remaining.set(i, ItemNBTHelper.getItemStack(stackInSlot, Lib.NBT_Powerpack));
		}
		return remaining;
	}
}