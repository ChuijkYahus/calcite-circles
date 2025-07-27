package miyucomics.calcite

import miyucomics.calcite.chalk.ActivatorItem
import miyucomics.calcite.chalk.ChalkBlock
import miyucomics.calcite.chalk.ChalkBlockEntity
import miyucomics.calcite.chalk.ChalkItem
import miyucomics.calcite.wave.WaveManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

class CalciteMain : ModInitializer {
	override fun onInitialize() {
		Registry.register(Registries.ITEM, id("activator"), ActivatorItem())

		Registry.register(Registries.ITEM, id("chalk"), ChalkItem())
		Registry.register(Registries.BLOCK, id("chalk"), CHALK_BLOCK)
		Registry.register(Registries.BLOCK_ENTITY_TYPE, id("chalk"), CHALK_BLOCK_ENTITY)
		ServerTickEvents.END_WORLD_TICK.register { WaveManager.tick(it) }
	}

	companion object {
		fun id(string: String) = Identifier("calcite", string)
		val PARTICLE_PACKET = id("effects")
		val CHALK_BLOCK: ChalkBlock = ChalkBlock()
		val CHALK_BLOCK_ENTITY: BlockEntityType<ChalkBlockEntity> = BlockEntityType.Builder.create(::ChalkBlockEntity, CHALK_BLOCK).build(null)
	}
}