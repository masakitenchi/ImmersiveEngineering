/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.render.tile;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.IEProperties.IEObjState;
import blusunrize.immersiveengineering.api.IEProperties.Model;
import blusunrize.immersiveengineering.api.IEProperties.VisibilityList;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.DynamicModelLoader;
import blusunrize.immersiveengineering.client.render.tile.DynamicModel.ModelType;
import blusunrize.immersiveengineering.client.utils.IERenderTypes;
import blusunrize.immersiveengineering.client.utils.SinglePropertyModelData;
import blusunrize.immersiveengineering.common.blocks.IEBlocks.Multiblocks;
import blusunrize.immersiveengineering.common.blocks.metal.ArcFurnaceTileEntity;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ArcFurnaceRenderer extends TileEntityRenderer<ArcFurnaceTileEntity>
{
	private TextureAtlasSprite hotMetal_flow = null;
	private TextureAtlasSprite hotMetal_still = null;
	private final DynamicModel<Direction> electrodes = DynamicModel.createSided(
			new ResourceLocation(ImmersiveEngineering.MODID, "block/metal_multiblock/arc_furnace_electrodes.obj.ie"),
			"arc_furnace_electrodes", ModelType.IE_OBJ);

	private static final ResourceLocation HOT_METLA_STILL = new ResourceLocation(ImmersiveEngineering.MODID, "block/fluid/hot_metal_still");
	private static final ResourceLocation HOT_METLA_FLOW = new ResourceLocation(ImmersiveEngineering.MODID, "block/fluid/hot_metal_flow");

	public ArcFurnaceRenderer(TileEntityRendererDispatcher rendererDispatcherIn)
	{
		super(rendererDispatcherIn);
		DynamicModelLoader.requestTexture(HOT_METLA_FLOW);
		DynamicModelLoader.requestTexture(HOT_METLA_STILL);
	}

	@Override
	public void render(ArcFurnaceTileEntity te, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer bufferIn,
					   int combinedLightIn, int combinedOverlayIn)
	{
		if(!te.formed||te.isDummy()||!te.getWorldNonnull().isBlockLoaded(te.getPos()))
			return;
		List<String> renderedParts = null;
		for(int i = 0; i < 3; i++)
			if(!te.getInventory().get(23+i).isEmpty())
			{
				if(renderedParts==null)
					renderedParts = Lists.newArrayList("electrode"+(i+1));
				else
					renderedParts.add("electrode"+(i+1));
			}
		if(renderedParts==null)
			return;
		if(te.shouldRenderAsActive())
			renderedParts.add("active");

		final BlockRendererDispatcher blockRenderer = Minecraft.getInstance().getBlockRendererDispatcher();
		BlockPos blockPos = te.getPos();
		BlockState state = te.getWorld().getBlockState(blockPos);
		if(state.getBlock()!=Multiblocks.arcFurnace)
			return;
		IBakedModel model = electrodes.get(te.getFacing());
		IEObjState objState = new IEObjState(VisibilityList.show(renderedParts));

		matrixStack.push();
		matrixStack.translate(.5, .5, .5);

		matrixStack.translate(-.5, -.5, -.5);
		blockRenderer.getBlockModelRenderer().renderModel(te.getWorldNonnull(), model, state, blockPos, matrixStack,
				bufferIn.getBuffer(RenderType.getSolid()), true, te.getWorld().rand, 0, 0,
				new SinglePropertyModelData<>(objState, Model.IE_OBJ_STATE));
		matrixStack.translate(.5, .5, .5);

		if(te.pouringMetal > 0)
		{
			if(hotMetal_flow==null)
			{
				AtlasTexture blockMap = ClientUtils.mc().getModelManager().getAtlasTexture(PlayerContainer.LOCATION_BLOCKS_TEXTURE);
				hotMetal_still = blockMap.getSprite(HOT_METLA_STILL);
				hotMetal_flow = blockMap.getSprite(HOT_METLA_FLOW);
			}
			matrixStack.rotate(new Quaternion(new Vector3f(0, 1, 0), -te.getFacing().getHorizontalAngle()+180, true));
			int process = 40;
			float speed = 5f;
			int pour = process-te.pouringMetal;
			float h = (pour > (process-speed)?((process-pour)/speed*27): pour > speed?27: (pour/speed*27))/16f;
			matrixStack.translate(-.5f, 1.25-.6875f, 1.5f);
			IVertexBuilder fullbright = bufferIn.getBuffer(IERenderTypes.SOLID_FULLBRIGHT);
			matrixStack.push();
			if(pour > (process-speed))
				matrixStack.translate(0, -1.6875f+h, 0);
			if(h > 1)
			{
				matrixStack.translate(0, -h, 0);
				ClientUtils.renderTexturedBox(fullbright, matrixStack, .375F, 0, .375F, .625F, 1, .625F, hotMetal_flow, true);
				matrixStack.translate(0, 1, 0);
				ClientUtils.renderTexturedBox(fullbright, matrixStack, .375F, 0, .375F, .625F, h-1, .625F, hotMetal_flow, true);
				matrixStack.translate(0, -1, 0);
				matrixStack.translate(0, h, 0);
			}
			else
			{
				matrixStack.translate(0, -h, 0);
				ClientUtils.renderTexturedBox(fullbright, matrixStack, .375F, 0, .375F, .625F, h, .625F, hotMetal_flow, true);
				matrixStack.translate(0, h, 0);
			}
			if(pour > (process-speed))
				matrixStack.translate(0, 1.6875f-h, 0);
			if(pour > speed)
			{
				float h2 = (pour > (process-speed)?.625f: pour/(process-speed)*.625f);
				matrixStack.translate(0, -1.6875f, 0);
				ClientUtils.renderTexturedBox(fullbright, matrixStack, .125F, 0, .125F, .875F, h2, .875F, hotMetal_still, false);
				matrixStack.translate(0, 1.6875f, 0);
			}
			matrixStack.pop();
		}
		matrixStack.pop();
	}
}