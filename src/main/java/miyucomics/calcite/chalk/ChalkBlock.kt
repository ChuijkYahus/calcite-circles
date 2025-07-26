package miyucomics.calcite.chalk

import net.minecraft.block.*
import net.minecraft.block.enums.WallMountLocation
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.DirectionProperty
import net.minecraft.state.property.EnumProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.BlockMirror
import net.minecraft.util.BlockRotation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.BlockView
import net.minecraft.world.WorldView

class ChalkBlock : Block(Settings.copy(Blocks.CALCITE).strength(1f, 1f).breakInstantly().noCollision()), BlockEntityProvider {
	init {
		this.defaultState = this.stateManager.getDefaultState().with(FACING, Direction.NORTH)
	}

	override fun getOutlineShape(state: BlockState, world: BlockView, pos: BlockPos, context: ShapeContext): VoxelShape {
		return when (state.get(ATTACH_FACE)) {
			WallMountLocation.FLOOR -> AABB_FLOOR
			WallMountLocation.CEILING -> AABB_CEILING
			WallMountLocation.WALL -> when (state.get(FACING)) {
				Direction.NORTH -> AABB_NORTH_WALL
				Direction.EAST -> AABB_EAST_WALL
				Direction.SOUTH -> AABB_SOUTH_WALL
				else -> AABB_WEST_WALL
			}
			else -> throw IllegalStateException()
		}
	}

	override fun getPlacementState(context: ItemPlacementContext): BlockState? {
		for (direction in context.placementDirections) {
			val state = if (direction.axis === Direction.Axis.Y)
				this.defaultState
					.with(ATTACH_FACE, if (direction == Direction.UP) WallMountLocation.CEILING else WallMountLocation.FLOOR)
					.with(FACING, context.horizontalPlayerFacing.opposite)
			else
				this.defaultState
					.with(ATTACH_FACE, WallMountLocation.WALL).with(FACING, direction.opposite)

			if (state.canPlaceAt(context.world, context.blockPos))
				return state
		}

		return null
	}

	override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
		super.appendProperties(builder)
		builder.add(FACING, ATTACH_FACE)
	}

	override fun getRenderType(state: BlockState) = BlockRenderType.INVISIBLE
	override fun isTransparent(state: BlockState, world: BlockView, pos: BlockPos) = true
	override fun createBlockEntity(pos: BlockPos, state: BlockState) = ChalkBlockEntity(pos, state)
	override fun mirror(state: BlockState, mirror: BlockMirror) = state.rotate(mirror.getRotation(state.get(FACING)))
	override fun rotate(state: BlockState, rotation: BlockRotation) = state.with(FACING, rotation.rotate(state.get(FACING)))
	override fun canPlaceAt(state: BlockState, world: WorldView, pos: BlockPos): Boolean {
		val surface = when (state.get(ATTACH_FACE)) {
			WallMountLocation.CEILING -> Direction.UP
			WallMountLocation.FLOOR -> Direction.DOWN
			else -> state.get(FACING).opposite
		}
		val mountLocation = pos.offset(surface)
		return world.getBlockState(mountLocation).isSideSolidFullSquare(world, mountLocation, surface)
	}

	companion object {
		val FACING: DirectionProperty = Properties.HORIZONTAL_FACING
		val ATTACH_FACE: EnumProperty<WallMountLocation> = Properties.WALL_MOUNT_LOCATION

		const val THICKNESS = 1.0
		val AABB_FLOOR: VoxelShape = createCuboidShape(0.0, 0.0, 0.0, 16.0, THICKNESS, 16.0)
		val AABB_CEILING: VoxelShape = createCuboidShape(0.0, 16 - THICKNESS, 0.0, 16.0, 16.0, 16.0)
		val AABB_EAST_WALL: VoxelShape = createCuboidShape(0.0, 0.0, 0.0, THICKNESS, 16.0, 16.0)
		val AABB_WEST_WALL: VoxelShape = createCuboidShape(16 - THICKNESS, 0.0, 0.0, 16.0, 16.0, 16.0)
		val AABB_SOUTH_WALL: VoxelShape = createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, THICKNESS)
		val AABB_NORTH_WALL: VoxelShape = createCuboidShape(0.0, 0.0, 16 - THICKNESS, 16.0, 16.0, 16.0)
	}
}
