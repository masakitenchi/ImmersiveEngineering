/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.lib.manual;

import blusunrize.immersiveengineering.client.utils.IERenderTypes;
import blusunrize.immersiveengineering.common.util.IELogger;
import blusunrize.lib.manual.Tree.AbstractNode;
import blusunrize.lib.manual.gui.GuiButtonManualLink;
import blusunrize.lib.manual.gui.ManualScreen;
import blusunrize.lib.manual.links.Link;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

public class ManualUtils
{
	public static boolean stackMatchesObject(ItemStack stack, Object o)
	{
		if(o instanceof ResourceLocation)
			return isInTag(stack, (ResourceLocation)o);
		if(o instanceof ItemStack)
		{
			if(!ItemStack.areItemsEqual((ItemStack)o, stack))
				return false;
			if(((ItemStack)o).hasTag())
				return ((ItemStack)o).getTag().equals(stack.getTag());
			return true;
		}
		return false;
	}

	public static boolean isInTag(ItemStack stack, ResourceLocation tag)
	{
		Tag<Item> itemTag = ItemTags.getCollection().get(tag);
		if(itemTag!=null&&itemTag.contains(stack.getItem()))
			return true;
		Tag<Block> blockTag = BlockTags.getCollection().get(tag);
		return blockTag!=null&&blockTag.contains(Block.getBlockFromItem(stack.getItem()));
	}

	public static boolean isNonemptyItemTag(ResourceLocation name)
	{
		Tag<Item> t = ItemTags.getCollection().getTagMap().get(name);
		return t!=null&&!t.getAllElements().isEmpty();
	}

	public static boolean isNonemptyBlockTag(ResourceLocation name)
	{
		Tag<Block> t = BlockTags.getCollection().getTagMap().get(name);
		return t!=null&&!t.getAllElements().isEmpty();
	}

	public static boolean isNonemptyBlockOrItemTag(ResourceLocation name)
	{
		return isNonemptyBlockTag(name)||isNonemptyItemTag(name);
	}

	public static String getTitleForNode(AbstractNode<ResourceLocation, ManualEntry> node, ManualInstance inst)
	{
		if(node.isLeaf())
			return inst.formatEntryName(node.getLeafData().getTitle());
		else
			return inst.formatCategoryName(node.getNodeData());
	}

	public static void drawTexturedRect(ResourceLocation texture, int x, int y, int w, int h, float... uv)
	{
		IRenderTypeBuffer.Impl buffers = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
		drawTexturedRect(new MatrixStack(), buffers, texture, x, y, w, h, uv);
		buffers.finish();
	}

	public static void drawTexturedRect(MatrixStack transform, IRenderTypeBuffer buffers, ResourceLocation texture,
										int x, int y, int w, int h, float... uv)
	{
		IVertexBuilder buffer = buffers.getBuffer(IERenderTypes.getGui(texture));
		Matrix4f mat = transform.getLast().getMatrix();
		buffer.pos(mat, x, y+h, 0)
				.color(1F, 1F, 1F, 1F)
				.tex(uv[0], uv[3])
				.endVertex();
		buffer.pos(mat, x+w, y+h, 0)
				.color(1F, 1F, 1F, 1F)
				.tex(uv[1], uv[3])
				.endVertex();
		buffer.pos(mat, x+w, y, 0)
				.color(1F, 1F, 1F, 1F)
				.tex(uv[1], uv[2])
				.endVertex();
		buffer.pos(mat, x, y, 0)
				.color(1F, 1F, 1F, 1F)
				.tex(uv[0], uv[2])
				.endVertex();
	}

	public static <T> List<T> getPrimitiveSpellingCorrections
			(String query, Iterable<T> valid, int maxDistance, Function<T, String> getTitle)
	{
		List<T> ret = new ArrayList<>();
		for(T node : valid)
		{
			String s = getTitle.apply(node);
			if(s!=null&&!s.trim().isEmpty())
				if(getSpellingDistanceBetweenStrings(query, s) < maxDistance)
					ret.add(node);
		}
		ret.sort(
				Comparator.comparingInt(s -> getSpellingDistanceBetweenStrings(query, getTitle.apply(s)))
		);
		return ret;
	}

	public static int getSpellingDistanceBetweenStrings(String query, String target)
	{
		query = query.toLowerCase(Locale.ENGLISH);
		target = target.toLowerCase(Locale.ENGLISH);

		String[] queryWords = query.split(" ");
		String[] targetWords = target.split(" ");
		int distance = 0;
		for(int iWord = 0; iWord < queryWords.length; iWord++)
		{
			if(iWord >= targetWords.length)
				distance++;
			else
			{
				int wordDistance = 0;
				for(int iChar = 0; iChar < queryWords[iWord].length(); iChar++)
					if(iChar >= targetWords[iWord].length())
						distance++;
					else
					{
						if(queryWords[iWord].charAt(iChar)!=targetWords[iWord].charAt(iChar))
						{
							wordDistance++;
							if(iChar > 0
									&&queryWords[iWord].charAt(iChar-1)==targetWords[iWord].charAt(iChar)
									&&queryWords[iWord].charAt(iChar)==targetWords[iWord].charAt(iChar-1))
								wordDistance -= 2;//switched letters don't increase distance
						}
					}
				if(wordDistance > 0)
					wordDistance += Math.abs(targetWords[iWord].length()-queryWords[iWord].length());
				distance += wordDistance;
			}
		}
		return distance;
	}

	public static final String THIS = "this";

	public static void addLinkButtons(ManualEntry entry, ManualInstance manual, ManualScreen gui, List<String> text, int x, int y,
									  List<Button> pageButtons, List<Link> repList)
	{
		for(Link link : repList)
		{
			List<GuiButtonManualLink> parts = new ArrayList<>();
			for(String partText : link.getParts())
				for(int line = 0; line < text.size(); line++)
				{
					String s = text.get(line);
					int start;
					if((start = s.indexOf(partText.trim())) >= 0)
					{
						String linkText = partText;
						if(!s.substring(start).startsWith(linkText))//This can happen when whitespace is cut off at the end of a line
							linkText = linkText.trim();
						int bx = manual.fontRenderer().getStringWidth(s.substring(0, start));
						int by = line*manual.fontRenderer().FONT_HEIGHT;
						ResourceLocation bkey = link.getTarget(entry);
						int bw = manual.fontRenderer().getStringWidth(linkText);
						ManualInstance.ManualLink outputLink;
						ManualEntry bEntry = manual.getEntry(bkey);
						if(bEntry!=null&&bEntry.hasAnchor(link.getTargetAnchor()))
							outputLink = new ManualInstance.ManualLink(bEntry, link.getTargetAnchor(), link.getTargetOffset());
						else
						{
							if(bEntry==null)
								IELogger.logger.error("Unknown manual entry: {} (link from {})", bkey, entry.getLocation());
							else if(!bEntry.hasAnchor(link.getTargetAnchor()))
								IELogger.logger.error("Unknown anchor {} in entry {} (link from {})", link.getTargetAnchor(), bkey,
										entry.getLocation());
							outputLink = null;
						}
						GuiButtonManualLink btn = new GuiButtonManualLink(gui, x+bx, y+by, bw, (int)(manual.fontRenderer().FONT_HEIGHT*1.5),
								outputLink, linkText);
						parts.add(btn);
						pageButtons.add(btn);
						text.set(line, s);
						break;
					}
				}
			for(GuiButtonManualLink btn : parts)
				btn.otherParts = parts;
		}
	}

	public static String attemptStringTranslation(String tranlationKey, String arg)
	{
		String untranslated = String.format(tranlationKey, arg);
		String translated = I18n.format(untranslated);
		if(!untranslated.equals(translated))
			return translated;
		return arg;
	}

	static HashMap<String, ResourceLocation> resourceMap = new HashMap<>();

	public static Tessellator tes()
	{
		return Tessellator.getInstance();
	}

	public static Minecraft mc()
	{
		return Minecraft.getInstance();
	}

	public static void bindTexture(ResourceLocation path)
	{
		mc().getTextureManager().bindTexture(path);
	}

	public static ResourceLocation getResource(String path)
	{
		ResourceLocation rl = resourceMap.containsKey(path)?resourceMap.get(path): new ResourceLocation(path);
		if(!resourceMap.containsKey(path))
			resourceMap.put(path, rl);
		return rl;
	}

	public static ItemRenderer renderItem()
	{
		return mc().getItemRenderer();
	}

	/**
	 * Custom implementation of drawing a split string because Mojang's doesn't reset text colour between lines >___>
	 */
	public static void drawSplitString(FontRenderer fontRenderer, List<String> text, int x, int y, int colour)
	{
		for(String s : text)
		{
			fontRenderer.drawString(s, x, y, colour);
			y += fontRenderer.FONT_HEIGHT;
		}
	}

	private static void parseSpecial(JsonObject obj, String anchor, TextSplitter splitter, ManualInstance instance)
	{
		String type = JSONUtils.getString(obj, "type");
		int offset = JSONUtils.getInt(obj, "offset", 0);
		ResourceLocation resLoc = getLocationForManual(type, instance);
		Function<JsonObject, SpecialManualElement> createElement = instance.getElementFactory(resLoc);
		splitter.addSpecialPage(anchor, offset, createElement.apply(obj));
	}

	public static void parseSpecials(JsonObject data, TextSplitter splitter, ManualInstance instance)
	{
		for(Entry<String, JsonElement> entry : data.entrySet())
		{
			JsonElement currData = entry.getValue();
			if(currData.isJsonObject())
				parseSpecial(currData.getAsJsonObject(), entry.getKey(), splitter, instance);
			else
				for(JsonElement inner : currData.getAsJsonArray())
					parseSpecial(inner.getAsJsonObject(), entry.getKey(), splitter, instance);
		}
	}

	public static ResourceLocation getLocationForManual(String s, ManualInstance instance)
	{
		if(s.indexOf(':') >= 0)
			return new ResourceLocation(s);
		else
			return new ResourceLocation(instance.getDefaultResourceDomain(), s);
	}

	public static boolean isNumber(JsonObject main, String name)
	{
		return main.has(name)&&main.get(name).isJsonPrimitive()&&main.get(name).getAsJsonPrimitive().isNumber();
	}

	@Nullable
	public static PositionedItemStack parsePosItemStack(JsonElement ele)
	{
		JsonObject json = ele.getAsJsonObject();
		if(!isNumber(json, "x"))
			return null;
		int x = JSONUtils.getInt(json, "x");
		if(!isNumber(json, "y"))
			return null;
		int y = JSONUtils.getInt(json, "y");
		if(JSONUtils.isString(json, "item"))
			return new PositionedItemStack(CraftingHelper.getItemStack(json, true), x, y);
		else if(JSONUtils.isJsonArray(json, "stacks"))
		{
			JsonArray arr = json.getAsJsonArray("stacks");
			List<ItemStack> stacks = new ArrayList<>(arr.size());
			for(JsonElement stack : arr)
				stacks.add(CraftingHelper.getItemStack(stack.getAsJsonObject(), true));
			return new PositionedItemStack(stacks, x, y);
		}
		else
			try
			{
				return new PositionedItemStack(CraftingHelper.getIngredient(json), x, y);
			} catch(JsonSyntaxException xcp)
			{
				return null;
			}
	}

	public static ItemStack getItemStackFromJson(ManualInstance m, JsonElement jsonEle)
	{
		if(jsonEle.isJsonPrimitive())
		{
			ResourceLocation itemName = getLocationForManual(jsonEle.getAsString(), m);
			return new ItemStack(ForgeRegistries.ITEMS.getValue(itemName));
		}
		else
			return CraftingHelper.getItemStack(jsonEle.getAsJsonObject(), true);
	}

	public static Object getRecipeObjFromJson(ManualInstance m, JsonElement jsonEle)
	{
		if(jsonEle.isJsonObject())
		{
			JsonObject json = jsonEle.getAsJsonObject();
			if(JSONUtils.isString(json, "recipe"))
				return ManualUtils.getLocationForManual(JSONUtils.getString(json, "recipe"), m);
			else if(JSONUtils.isString(json, "orename"))
				return json.get("orename").getAsString();
			else if(JSONUtils.isString(json, "item"))
				return CraftingHelper.getItemStack(json, true);
		}
		else if(jsonEle.isJsonArray())
		{
			JsonArray json = jsonEle.getAsJsonArray();
			PositionedItemStack[] stacks = new PositionedItemStack[json.size()];
			for(int i = 0; i < json.size(); i++)
			{
				PositionedItemStack posStack = parsePosItemStack(json.get(i));
				if(posStack!=null)
					stacks[i] = posStack;
				else
					throw new RuntimeException("Failed to load positional item stack from "+json.get(i));
			}
			return stacks;
		}
		else if(jsonEle.isJsonPrimitive()&&jsonEle.getAsJsonPrimitive().isString())
			return ManualUtils.getLocationForManual(jsonEle.getAsString(), m);
		throw new RuntimeException("Could not find recipe for "+jsonEle);
	}

	public static boolean listStack(String search, ItemStack stack)
	{
		return stack.getDisplayName().getFormattedText().toLowerCase(Locale.ENGLISH).contains(search);
	}
}
