package miyucomics.calcite

import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier

class CalciteMain : ModInitializer {
	override fun onInitialize() {

	}

	companion object {
		fun id(string: String) = Identifier("calcite", string)
	}
}