package miyucomics.calcite.wave

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.utils.putList
import miyucomics.calcite.CalciteMain
import miyucomics.calcite.chalk.ChalkBlock
import miyucomics.calcite.chalk.ChalkBlockEntity
import net.minecraft.block.enums.WallMountLocation
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import org.joml.Vector2d
import org.joml.Vector2i
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class Wave(val origin: BlockPos, val visited: MutableList<BlockPos>, val iotas: MutableList<NbtCompound>, val media: Long, val chalkNormal: Direction, var travelDirection: Direction) {
	fun tick(world: ServerWorld) {
		if (visited.isEmpty())
			return
		val lastBlock = visited.last()

		if (travelDirection.axis == chalkNormal.axis) {
			println("something has gone horribly wrong")
			return
		}

		val particlePosition = lastBlock.toCenterPos()
		world.spawnParticles(ParticleTypes.SMOKE, particlePosition.x, particlePosition.y, particlePosition.z, 1, 0.0, 0.0, 0.0, 0.0)

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

			if (nextBlock == origin) {
				println(getHex(world))
				visited.clear()
				visited.add(origin)
				iotas.clear()
				iotas.add((world.getBlockEntity(origin) as ChalkBlockEntity).iota)
				return
			}

			if (!visited.contains(nextBlock)) {
				visited.add(nextBlock)
				iotas.add((world.getBlockEntity(nextBlock) as ChalkBlockEntity).iota)
				travelDirection = direction
				return
			}
		}

		println("Wave terminated at $lastBlock (no valid chalk blocks found).")
		visited.clear()
	}

	fun getHex(world: ServerWorld) = iotas.map { IotaType.deserialize(it, world) }

	fun hasAmbit(test: Vec3d): Boolean {
		val firstBlock = visited[0]
		val (plane, planeCoordinate) = when {
			visited.all { it.x == firstBlock.x } -> Plane.YZ to firstBlock.x.toDouble()
			visited.all { it.y == firstBlock.y } -> Plane.XZ to firstBlock.y.toDouble()
			visited.all { it.z == firstBlock.z } -> Plane.XY to firstBlock.z.toDouble()
			else -> return false
		}

		val distanceToPlane = when (plane) {
			Plane.YZ -> abs(test.x - planeCoordinate)
			Plane.XZ -> abs(test.y - planeCoordinate)
			Plane.XY -> abs(test.z - planeCoordinate)
		}
		if (distanceToPlane > 2.0)
			return false

		val projectedPoint = when (plane) {
			Plane.YZ -> Vector2d(test.y, test.z)
			Plane.XZ -> Vector2d(test.x, test.z)
			Plane.XY -> Vector2d(test.x, test.y)
		}

		var intersections = 0
		val numPoints = visited.size

		for (i in 0 until numPoints) {
			val a = plane.projectVector(visited[i])
			val b = plane.projectVector(visited[(i + 1) % numPoints])
			val (segA, segB) = if (a.y < b.y) a to b else b to a

			// Case 1: Vertical Edge
			// Check if the ray's y is within the y-range of the vertical segment
			// AND if the ray's x is less than the segment's x ( segment is to the right )
			if (segA.x == segB.x) {
				if (projectedPoint.y >= segA.y && projectedPoint.y < segB.y && projectedPoint.x < segA.x)
					intersections++
			}
			// Case 2: Horizontal Edge
			// A horizontal ray either runs along or passes above/below a horizontal edge
			// We need to handle the case where the point is exactly on a vertex or horizontal edge.
			// If the ray's y is exactly on this horizontal segment's y, check if the point's x is within the segment's x range
			// If so, point is on the boundary. Let's say that this is in ambit.
			else if (segA.y == segB.y) {
				if (projectedPoint.y == segA.y.toDouble() && projectedPoint.x >= min(segA.x, segB.x) && projectedPoint.x <= max(segA.x, segB.x))
					return true
			}
		}

		return intersections % 2 == 1
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

			val visitedInts = nbt.getIntArray("visited")
			val visited = visitedInts.toList()
				.chunked(3)
				.map { (x, y, z) -> BlockPos(x, y, z) }

			val iotaList = nbt.getList("iotas", NbtElement.COMPOUND_TYPE.toInt())
			val iotas = MutableList(iotaList.size) { iotaList.getCompound(it) }

			return Wave(origin, visited.toMutableList(), iotas, nbt.getLong("media"), Direction.byId(nbt.getInt("chalkNormal")), Direction.byId(nbt.getInt("travelDirection")))
		}
	}
}

enum class Plane {
	XY, XZ, YZ
}

private fun Direction.toPlane() = when (this.axis) {
	Direction.Axis.X -> Plane.YZ
	Direction.Axis.Y -> Plane.XZ
	Direction.Axis.Z -> Plane.XY
}

private fun Plane.projectVector(vector: BlockPos) = when (this) {
	Plane.YZ -> Vector2i(vector.y, vector.z)
	Plane.XZ -> Vector2i(vector.x, vector.z)
	Plane.XY -> Vector2i(vector.x, vector.y)
}