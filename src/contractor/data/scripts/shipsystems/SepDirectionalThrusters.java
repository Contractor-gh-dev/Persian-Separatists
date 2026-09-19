package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;

public class SepDirectionalThrusters extends BaseShipSystemScript {
	private final HashMap<ShipEngineControllerAPI.ShipEngineAPI, Float> enginesPower = new HashMap<>();
	private static final float SPEEDBUFF = 25f;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
			stats.getMaxTurnRate().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyFlat(id, SPEEDBUFF);
			stats.getAcceleration().modifyPercent(id, 300f * effectLevel);
			stats.getDeceleration().modifyPercent(id, 300f * effectLevel);
			stats.getTurnAcceleration().modifyFlat(id, 40f * effectLevel);
			stats.getTurnAcceleration().modifyPercent(id, 300f * effectLevel);
			stats.getMaxTurnRate().modifyFlat(id, 10f);
			stats.getMaxTurnRate().modifyPercent(id, 50f);
		}

		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null)
			return;

		ShipEngineControllerAPI controller = ship.getEngineController();
		for (ShipEngineControllerAPI.ShipEngineAPI e : controller.getShipEngines()) {
			if (e.isSystemActivated())
				enginesPower.putIfAbsent(e, 0f);
		}
		if (enginesPower.isEmpty())
			return;

		float delta = Global.getCombatEngine().getElapsedInLastFrame();

		if (state == State.IN) {
			if (effectLevel > 0.2f) {
				computeEnginesIdle(controller, delta * 2f);
			}
			return;
		}

		if (state == State.ACTIVE) {
			float x = 0, y = 0;
			if (controller.isStrafingLeft())
				x -= 1;
			if (controller.isStrafingRight())
				x += 1;
			if (controller.isAcceleratingBackwards())
				y -= 1;
			if (controller.isAccelerating())
				y += 1;
			Vector2f moveVect = new Vector2f(x, y);

			if (moveVect.length() == 0) {
				computeEnginesIdle(controller, delta * 4f);
				return;
			}

			computeEngines(moveVect, controller, delta * 6f);
			return;
		}

		if (state == State.OUT) {
			computeEnginesIdle(controller, delta * 3f);
		}
	}

	private void computeEnginesIdle(ShipEngineControllerAPI controller, float delta) {
		for (ShipEngineControllerAPI.ShipEngineAPI e : controller.getShipEngines()) {
			if (e.isSystemActivated()) {
				enginesPower.put(e, enginesPower.get(e) - delta);
				enginesPower.forEach((shipEngineAPI, power) -> {
					if (power < 0f)
						enginesPower.put(shipEngineAPI, 0f);
					if (power > 1f)
						enginesPower.put(shipEngineAPI, 1f);
				});
				float level = enginesPower.get(e);
				controller.setFlameLevel(e.getEngineSlot(), level);
				e.getEngineSlot().setGlowSizeMult(level);
			}
		}
	}

	private void computeEngines(Vector2f moveVect, ShipEngineControllerAPI controller, float delta) {
		float moveAngle = MathUtils.clampAngle(VectorUtils.getFacing(moveVect) - 90f);

		for (ShipEngineControllerAPI.ShipEngineAPI e : controller.getShipEngines()) {
			if (e.isSystemActivated()) {
				if (Math.abs(MathUtils.getShortestRotation(moveAngle, e.getEngineSlot().getAngle())) > 90f) {
					enginesPower.put(e, enginesPower.get(e) + delta);
					e.getEngineSlot().setGlowSizeMult(enginesPower.get(e));
				} else
					enginesPower.put(e, enginesPower.get(e) - delta);
				e.getEngineSlot().setGlowSizeMult(enginesPower.get(e));
			}
		}

		enginesPower.forEach((shipEngineAPI, power) -> {
			if (power < 0f)
				enginesPower.put(shipEngineAPI, 0f);
			if (power > 1f)
				enginesPower.put(shipEngineAPI, 1f);
		});

		for (ShipEngineControllerAPI.ShipEngineAPI e : controller.getShipEngines()) {
			if (e.isSystemActivated()) {
				controller.setFlameLevel(e.getEngineSlot(), enginesPower.get(e));
			}
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("improved maneuverability", false);
		} else if (index == 1) {
			return new StatusData("+" + Math.round(SPEEDBUFF) + " top speed", false);
		}
		return null;
	}
}
