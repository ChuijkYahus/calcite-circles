package miyucomics.calcite

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.client.ScryingLensOverlayRegistry
import miyucomics.calcite.chalk.ChalkBlockEntity
import miyucomics.calcite.chalk.ChalkBlockEntityRenderer
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories

class CalciteClient : ClientModInitializer {
	override fun onInitializeClient() {
		BlockEntityRendererFactories.register(CalciteMain.CHALK_BLOCK_ENTITY) { ChalkBlockEntityRenderer() }
		ScryingLensOverlayRegistry.addDisplayer(CalciteMain.CHALK_BLOCK) { lines, _, pos, _, world, _ ->
			IotaType.getDisplay((world.getBlockEntity(pos) as ChalkBlockEntity).iota)
		}
	}
}