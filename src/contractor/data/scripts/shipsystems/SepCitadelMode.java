package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.statuseffects.StatusEffect;
import contractor.data.scripts.statuseffects.StatusEffectUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SepCitadelMode extends BaseShipSystemScript {
	private static final float MAX_SPEED = 0.1f, ACC = 0.1f, MAX_TURN = 0.5f, TURN_ACC = 0.5f;
	private static final Color VENT_COLOR = new Color(50, 0, 155, 150);
	private static final Color COLD_L1 = new Color(125, 125, 255, 200);
	private static final Color COLD_L2 = new Color(175, 175, 255, 255);
	private boolean initial = true;
	public float coolantUsed = 0f;


	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused())
			return;
		float rechargeRate;
		if (stats.getEntity() instanceof ShipAPI ship)
			rechargeRate = engine.getElapsedInLastFrame() * 0.4f * stats.getSystemRegenBonus().getBonusMult();
		else
			return;

		if (effectLevel <= 0f) {
			coolantUsed -= rechargeRate;
			if (coolantUsed < 0f)
				coolantUsed = 0f;
			unapply(stats, id);
			return;
		}

		float size = (float) Math.random() * 48f + 16f;
		for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
			if (slot.isSystemSlot()) {
				float angRand = (float) (Math.random() - 0.5f) * (9f + coolantUsed / 3f);
				Vector2f aimVec = new Vector2f(Misc.getUnitVectorAtDegreeAngle(slot.computeMidArcAngle(ship) + angRand));
				VectorUtils.resize(aimVec, (float) Math.random() * 64f + 96f + (coolantUsed * 3f), aimVec);
				Global.getCombatEngine().addHitParticle(slot.computePosition(ship), aimVec, size * 0.5f, 0.75f, 0.8f, VENT_COLOR);
				if (coolantUsed > 22f)
					Global.getCombatEngine().addHitParticle(slot.computePosition(ship), aimVec, size * 0.4f, 0.75f, 0.7f, COLD_L2);
				else if (coolantUsed > 11f)
					Global.getCombatEngine().addHitParticle(slot.computePosition(ship), aimVec, size * 0.4f, 0.75f, 0.7f, COLD_L1);
			}
		}

		if (effectLevel < 1f) {
			if (initial) {
				coolantUsed += 1f;
				initial = false;
			}
			stats.getMaxSpeed().modifyMult(id, MAX_SPEED * 2f);
			stats.getAcceleration().modifyMult(id, ACC * 2f);
			stats.getMaxTurnRate().modifyMult(id, MAX_TURN * 1.5f);
			stats.getTurnAcceleration().modifyMult(id, TURN_ACC * 1.5f);
			return;
		}

		coolantUsed += engine.getElapsedInLastFrame() * (1f / stats.getSystemRegenBonus().getBonusMult());

		stats.getMaxSpeed().modifyMult(id, MAX_SPEED);
		stats.getAcceleration().modifyMult(id, ACC);
		stats.getMaxTurnRate().modifyMult(id, MAX_TURN);
		stats.getTurnAcceleration().modifyMult(id, TURN_ACC);
		stats.getFluxDissipation().modifyMult(id, 2f);

		if (coolantUsed >= 33.33) {
			ship.getFluxTracker().beginOverloadWithTotalBaseDuration(3f);
			ship.getSystem().deactivate();
			ship.getSystem().setCooldownRemaining(10f);
			StatusEffectUtils.addOrMaintainEffect(ship, StatusEffect.StatusType.CRYOGENIC, 10f, 10f);
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		initial = true;
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getFluxDissipation().unmodify(id);
	}

	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isChargeup())
			return "CHARGING | COOLANT: " + Math.round(100f - coolantUsed * 3f);
		if (coolantUsed == 0f)
			return "READY";
		if (ship.getSystem().getState() == ShipSystemAPI.SystemState.IDLE)
			return "READY | COOLANT: " + Math.round(100f - coolantUsed * 3f);
		if (system.isCoolingDown())
			return "RESTARTING REACTOR";
		if (system.isActive())
			return "ACTIVE | COOLANT: " + Math.round(100f - coolantUsed * 3f);

		return null;
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (effectLevel > 0.5f) {
			if (index == 0)
				return new StatusData("Engines Offline", true);
			else if (index == 1)
				return new StatusData("Flux Dissipation Doubled", false);
		}
		return null;
	}
}
