package miyucomics.calcite

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.client.ScryingLensOverlayRegistry
import at.petrak.hexcasting.api.mod.HexConfig.client
import com.mojang.datafixers.util.Pair
import miyucomics.calcite.chalk.ChalkBlockEntity
import miyucomics.calcite.chalk.ChalkBlockEntityRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.block.Blocks
import net.minecraft.client.particle.BlockDustParticle
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories
import net.minecraft.item.ItemStack
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class CalciteClient : ClientModInitializer {
	override fun onInitializeClient() {
		BlockEntityRendererFactories.register(CalciteMain.CHALK_BLOCK_ENTITY) { ChalkBlockEntityRenderer() }
		ScryingLensOverlayRegistry.addDisplayer(CalciteMain.CHALK_BLOCK) { lines, _, pos, _, world, _ ->
			lines.add(Pair(ItemStack.EMPTY, IotaType.getDisplay((world.getBlockEntity(pos) as ChalkBlockEntity).iota)))
		}

		ClientPlayNetworking.registerGlobalReceiver(CalciteMain.PARTICLE_PACKET) { client, _, buf, _ ->
			val pos = Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble())
			val blockPos = BlockPos.ofFloored(pos)
			client.particleManager.addParticle(BlockDustParticle(client.world, pos.x, pos.y, pos.z, 0.5, 0.5, 0.5, Blocks.CALCITE.defaultState, blockPos))
			client.world!!.playSound(pos.x, pos.y, pos.z, SoundEvents.BLOCK_CALCITE_BREAK, SoundCategory.BLOCKS, 0.2f, 0.8f, false)
		}
	}
}