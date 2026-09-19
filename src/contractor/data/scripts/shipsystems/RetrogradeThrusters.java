package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;

import java.util.ArrayList;
import java.util.List;


public class RetrogradeThrusters extends BaseShipSystemScript {
	private static final float SPEEDMOD = 150f;
	private static final float TURNMOD = 0.1f;
	private static final float ACCMOD = 75f;
	private static final float ACCMODDN = 0.25f;


	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null || ship.isHulk())
			return;

		ShipEngineControllerAPI engines = ship.getEngineController();

		if (engines.isFlamedOut() && state == State.IN) {
			ship.getSystem().forceState(ShipSystemAPI.SystemState.OUT, 1f);
			return;
		}
		List<ShipEngineAPI> retroEngineList = new ArrayList<>();

		for (ShipEngineAPI e : engines.getShipEngines()) {
			if (e.isSystemActivated()) {
				if (state == State.ACTIVE)
					engines.setFlameLevel(e.getEngineSlot(), 1f);
				else if (state == State.OUT)
					engines.setFlameLevel(e.getEngineSlot(), effectLevel);
				else
					engines.setFlameLevel(e.getEngineSlot(), effectLevel * effectLevel);
				retroEngineList.add(e);
			} else {
				float flameLevel = Math.max(0f, 0.5f - effectLevel);
				engines.setFlameLevel(e.getEngineSlot(), flameLevel);
			}
		}

		if (state == State.OUT) {
			stats.getMaxSpeed().unmodify(id);
			stats.getMaxTurnRate().unmodify(id);
			stats.getAcceleration().unmodify(id);
			stats.getDeceleration().unmodify(id);
		} else {
			stats.getMaxTurnRate().modifyMult(id, TURNMOD);
			stats.getAcceleration().modifyMult(id, ACCMODDN);
			stats.getDeceleration().modifyMult(id, ACCMODDN);
		}

		if (state == State.ACTIVE) {
			for (ShipEngineAPI e1 : retroEngineList)
				e1.repair();

			stats.getMaxTurnRate().modifyMult(id, TURNMOD * 0f);
			stats.getMaxSpeed().modifyFlat(id, SPEEDMOD);
			stats.getAcceleration().modifyMult(id, ACCMOD);
			stats.getDeceleration().modifyMult(id, ACCMOD);

			ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
			ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
			ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
			ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
	}

	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (ship.getEngineController().isFlamedOut())
			return "FLAMED OUT";
		return switch (system.getState()) {
			case IDLE -> "READY";
			case IN -> "CHARGING";
			case ACTIVE, OUT -> "FIRING";
			case COOLDOWN -> "COOLING";
		};
	}
}

