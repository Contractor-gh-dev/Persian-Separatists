package contractor.data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

import static contractor.data.scripts.util.ContractorStaticVars.COMMAND_KEY;
import static contractor.data.scripts.util.ContractorStaticVars.MISSILE_COMMAND_DATAID;

public class PlayerMissileCommand implements EveryFrameCombatPlugin {
	private CombatEngineAPI engine;
	private ShipAPI playerShip;
	private int systemMode = -1;
	private boolean update = true;
	public final PlayerMissileCommandData data = new PlayerMissileCommandData();

	public static class PlayerMissileCommandData {
		public int systemMode = -1;
	}

	public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
		for (InputEventAPI in : events) {
			if (!in.isConsumed() && in.getEventValue() == COMMAND_KEY && in.isKeyDownEvent()) {
				systemMode++;
				update = true;
				if (systemMode == 5)
					systemMode = 6;
				if (systemMode > 6)
					systemMode = -1;
				break;
			}
		}
	}

	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		if (engine == null)
			engine = Global.getCombatEngine();

		if (playerShip != engine.getPlayerShip())
			playerShip = engine.getPlayerShip();

		if (!playerShip.getVariant().hasHullMod("contractor_missilecommand"))
			return;

		if (update) {
			update = false;
			data.systemMode = systemMode;
			engine.getCustomData().put(MISSILE_COMMAND_DATAID, data);
		}

		String mode;
		switch (systemMode) {
			case -1 -> mode = "Smart";
			case 0 -> mode = "Flank";
			case 1 -> mode = "Side Attack";
			case 2 -> mode = "Direct";
			case 3 -> mode = "Tail Strike";
			case 4 -> mode = "Vulture";
			case 6 -> mode = "Laser Guided";
			default -> mode = "Error";
		}
		engine.maintainStatusForPlayerShip("missilecommand_player", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Command Mode", mode, false);
	}

	public void renderInWorldCoords(ViewportAPI viewport) {
	}

	public void renderInUICoords(ViewportAPI viewport) {
	}

	public void init(CombatEngineAPI engine) {
	}
}
