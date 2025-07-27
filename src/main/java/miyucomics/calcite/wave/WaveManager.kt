package miyucomics.calcite.wave

import at.petrak.hexcasting.api.utils.asCompound
import at.petrak.hexcasting.api.utils.putList
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.PersistentState

class WaveManager : PersistentState() {
	val waves: MutableList<Wave> = mutableListOf()

	fun tick(world: ServerWorld) {
		for (wave in getManager(world).waves)
			wave.tick(world)
		waves.removeIf { it.canRemove }
		markDirty()
	}

	override fun writeNbt(nbt: NbtCompound): NbtCompound {
		return nbt.apply {
			putList("waves", NbtList().apply {
				for (wave in waves)
					add(wave.toNbt())
			})
		}
	}

	companion object {
		private fun createFromNbt(nbt: NbtCompound): WaveManager {
			val state = WaveManager()
			nbt.getList("waves", NbtElement.COMPOUND_TYPE.toInt()).forEach { state.waves.add(Wave.fromNbt(it.asCompound)) }
			return state
		}

		fun getManager(world: ServerWorld): WaveManager = world.persistentStateManager.getOrCreate(::createFromNbt, ::WaveManager, "calcite")

		fun tick(world: ServerWorld) {
			getManager(world).tick(world)
		}
	}
}