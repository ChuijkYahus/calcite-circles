package miyucomics.calcite.casting

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus
import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect.DoMishap
import at.petrak.hexcasting.api.pigment.FrozenPigment
import miyucomics.calcite.wave.Plane
import miyucomics.calcite.wave.projectVector
import miyucomics.calcite.wave.toPlane
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import java.util.function.Predicate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ChalkCastEnv(world: ServerWorld, val origin: BlockPos, val visited: List<BlockPos>, val chalkNormal: Direction) : CastingEnvironment(world) {
	override fun getCastingEntity() = null
	override fun getCastingHand() = Hand.MAIN_HAND
	override fun setPigment(pigment: FrozenPigment?) = pigment
	override fun getMishapEnvironment() = ChalkMishapEnv(world)
	override fun mishapSprayPos(): Vec3d = origin.toCenterPos()
	override fun getPrimaryStacks() = emptyList<HeldItemInfo>()
	override fun isVecInRangeEnvironment(vec: Vec3d) = checkBounds(vec)
	override fun getPigment(): FrozenPigment = FrozenPigment.DEFAULT.get()
	override fun extractMediaEnvironment(cost: Long, simulate: Boolean) = 0L
	override fun getUsableStacks(mode: StackDiscoveryMode?) = emptyList<ItemStack>()
	override fun produceParticles(particles: ParticleSpray, colorizer: FrozenPigment) {}
	override fun hasEditPermissionsAtEnvironment(pos: BlockPos) = checkBounds(pos.toCenterPos())
	override fun replaceItem(stackOk: Predicate<ItemStack>, replaceWith: ItemStack, hand: Hand?) = false

	override fun printMessage(message: Text) {
		world.randomAlivePlayer!!.sendMessage(message, true)
	}

	override fun postExecution(result: CastResult) {
		super.postExecution(result)
		for (sideEffect in result.sideEffects) {
			if (sideEffect is DoMishap) {
				val msg = sideEffect.mishap.errorMessageWithName(this, sideEffect.errorCtx)
				if (msg != null)
					world.randomAlivePlayer!!.sendMessage(msg, true)
			}
		}
	}

	private fun checkBounds(test: Vec3d): Boolean {
		if (visited.contains(BlockPos.ofFloored(test)))
			return true

		val firstBlock = visited[0]
		val plane = chalkNormal.toPlane()

		val planeCoordinate = when (plane) {
			Plane.YZ -> firstBlock.x.toDouble()
			Plane.XZ -> firstBlock.y.toDouble()
			Plane.XY -> firstBlock.z.toDouble()
		}

		val distanceToPlane = when (plane) {
			Plane.YZ -> abs(test.x - planeCoordinate)
			Plane.XZ -> abs(test.y - planeCoordinate)
			Plane.XY -> abs(test.z - planeCoordinate)
		}

		if (distanceToPlane > 2.0)
			return false

		val projectedPoint = plane.projectVector(test)
		val projectedVisted = visited.map { plane.projectVector(it) }

		var intersections = 0
		val numPoints = visited.size

		for (i in 0 until numPoints) {
			val a = projectedVisted[i]
			val b = projectedVisted[(i + 1) % numPoints]
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
}