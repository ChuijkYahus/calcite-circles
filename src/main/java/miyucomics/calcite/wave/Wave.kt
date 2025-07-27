package miyucomics.calcite.wave

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.utils.putList
import miyucomics.calcite.CalciteMain
import miyucomics.calcite.casting.ChalkCastEnv
import miyucomics.calcite.chalk.ChalkBlock
import miyucomics.calcite.chalk.ChalkBlockEntity
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.block.enums.WallMountLocation
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d

class Wave(val origin: BlockPos, val visited: MutableList<BlockPos>, val iotas: MutableList<NbtCompound>, val media: Long, val chalkNormal: Direction, var travelDirection: Direction) {
	var canRemove = false

	fun tick(world: ServerWorld) {
		if (canRemove)
			return
		val lastBlock = visited.last()

		val effectPosition = Vec3d.ofCenter(lastBlock).subtract(chalkNormal.offsetX.toDouble() * 0.4, chalkNormal.offsetY.toDouble() * 0.4, chalkNormal.offsetZ.toDouble() * 0.4)
		for (player in world.getPlayers { it.squaredDistanceTo(effectPosition) < 100 })
			ServerPlayNetworking.send(player, CalciteMain.PARTICLE_PACKET, PacketByteBufs.create().apply {
				writeDouble(effectPosition.x)
				writeDouble(effectPosition.y)
				writeDouble(effectPosition.z)
			})

		val directionsToCheck = when (chalkNormal.toPlane()) {
			Plane.XY -> listOf(travelDirection, travelDirection.rotateClockwise(Direction.Axis.Z), travelDirection.rotateCounterclockwise(Direction.Axis.Z))
			Plane.XZ -> listOf(travelDirection, travelDirection.rotateClockwise(Direction.Axis.Y), travelDirection.rotateCounterclockwise(Direction.Axis.Y))
			Plane.YZ -> listOf(travelDirection, travelDirection.rotateClockwise(Direction.Axis.X), travelDirection.rotateCounterclockwise(Direction.Axis.X))
		}

		for (direction in directionsToCheck) {
			val nextBlock = lastBlock.offset(direction)

			val state = world.getBlockState(nextBlock)
			if (!state.isOf(CalciteMain.CHALK_BLOCK))
				continue

			val chalkFacing = when (state.get(ChalkBlock.ATTACH_FACE)) {
				WallMountLocation.WALL -> state.get(ChalkBlock.FACING)
				WallMountLocation.FLOOR -> Direction.UP
				WallMountLocation.CEILING -> Direction.DOWN
			}

			if (chalkFacing != chalkNormal)
				continue

			travelDirection = direction

			if (nextBlock == origin) {
				cast(world)
				visited.clear()
				visited.add(origin)
				iotas.clear()
				iotas.add((world.getBlockEntity(origin) as ChalkBlockEntity).iota)
				return
			}

			if (!visited.contains(nextBlock)) {
				iotas.add((world.getBlockEntity(lastBlock) as ChalkBlockEntity).iota)
				visited.add(nextBlock)
				return
			}
		}

		canRemove = true
	}

	fun cast(world: ServerWorld) {
		val vm = CastingVM(CastingImage().copy(stack = origin.asActionResult), ChalkCastEnv(world, origin, visited, chalkNormal))
		vm.queueExecuteAndWrapIotas(iotas.map { IotaType.deserialize(it, world) }, world)
	}

	fun toNbt(): NbtCompound {
		return NbtCompound().apply {
			putIntArray("origin", intArrayOf(origin.x, origin.y, origin.z))
			val visitedInts = mutableListOf<Int>()
			visited.flatMapTo(visitedInts) { listOf(it.x, it.y, it.z) }
			putIntArray("visited", visitedInts)
			putList("iotas", NbtList().apply { iotas.forEach { it -> add(it) } })
			putLong("media", media)
			putInt("chalkNormal", chalkNormal.id)
			putInt("travelDirection", travelDirection.id)
		}
	}

	companion object {
		fun fromNbt(nbt: NbtCompound): Wave {
			val originInts = nbt.getIntArray("origin")
			val origin = BlockPos(originInts[0], originInts[1], originInts[2])

			val visitedBlocks = nbt.getIntArray("visited").toList()
				.chunked(3)
				.map { (x, y, z) -> BlockPos(x, y, z) }
				.toMutableList()

			val iotaList = nbt.getList("iotas", NbtElement.COMPOUND_TYPE.toInt())
			val iotas = MutableList(iotaList.size) { iotaList.getCompound(it) }

			return Wave(origin, visitedBlocks, iotas, nbt.getLong("media"), Direction.byId(nbt.getInt("chalkNormal")), Direction.byId(nbt.getInt("travelDirection")))
		}
	}
}