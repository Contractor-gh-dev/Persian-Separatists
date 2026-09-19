package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.PhaseCloakSystemAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class DrgLinearPhaseCloakStats extends BaseShipSystemScript {
	public static final float SHIP_ALPHA_MULT = 0.22f;

	public static final float MAX_TIME_MULT = 2f;

	public static final boolean FLUX_LEVEL_AFFECTS_SPEED = true;
	public static final float MIN_SPEED_MULT = 0.66f;
	public static final float BASE_FLUX_LEVEL_FOR_MIN_SPEED = 0.75f;

	protected final Object STATUSKEY1 = new Object();
	protected final Object STATUSKEY2 = new Object();

	public static float getMaxTimeMult(MutableShipStatsAPI stats) {
		return 1f + (MAX_TIME_MULT - 1f) * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
	}

	protected boolean isDisruptable(ShipSystemAPI cloak) {
		return cloak.getSpecAPI().hasTag(Tags.DISRUPTABLE);
	}

	protected float getDisruptionLevel(ShipAPI ship) {
		if (FLUX_LEVEL_AFFECTS_SPEED) {
			float threshold = ship.getMutableStats().getDynamic().getMod(
					Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).computeEffective(BASE_FLUX_LEVEL_FOR_MIN_SPEED);
			if (threshold <= 0) return 1f;
			float level = ship.getHardFluxLevel() / threshold;
			if (level > 1f) level = 1f;
			return level;
		}
		return 0f;
	}

	protected void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {
		ShipSystemAPI cloak = playerShip.getPhaseCloak();
		if (cloak == null) cloak = playerShip.getSystem();
		if (cloak == null) return;

		if (effectLevel > 0) {
			Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
					cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "time flow altered", false);
		}

		if (FLUX_LEVEL_AFFECTS_SPEED) {
			if (effectLevel > 0) {
				if (getDisruptionLevel(playerShip) <= 0f) {
					Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
							cloak.getSpecAPI().getIconSpriteName(), "phase coils stable", "top speed at 100%", false);
				} else {
					String speedPercentStr = (int) Math.round(getSpeedMult(playerShip, effectLevel) * 100f) + "%";
					Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
							cloak.getSpecAPI().getIconSpriteName(),
							"phase coil stress",
							"top speed at " + speedPercentStr, true);
				}
			}
		}
	}

	public float getSpeedMult(ShipAPI ship, float effectLevel) {
		if (getDisruptionLevel(ship) <= 0f) return 1f;
		return MIN_SPEED_MULT + (1f - MIN_SPEED_MULT) * (1f - getDisruptionLevel(ship) * effectLevel);
	}

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
			id = id + "_" + ship.getId();
		} else {
			return;
		}

		if (player) {
			maintainStatus(ship, state, effectLevel);
		}

		if (Global.getCombatEngine().isPaused()) {
			return;
		}

		ShipSystemAPI cloak = ship.getPhaseCloak();
		if (cloak == null) cloak = ship.getSystem();
		if (cloak == null) return;

		if (FLUX_LEVEL_AFFECTS_SPEED) {
			if (state == State.ACTIVE || state == State.OUT || state == State.IN) {
				float mult = getSpeedMult(ship, effectLevel);
				if (mult < 1f) {
					stats.getMaxSpeed().modifyMult(id + "_2", mult);
				} else {
					stats.getMaxSpeed().unmodifyMult(id + "_2");
				}
				((PhaseCloakSystemAPI) cloak).setMinCoilJitterLevel(getDisruptionLevel(ship));
			}
		}

		if (state == State.COOLDOWN || state == State.IDLE) {
			unapply(stats, id);
			return;
		}

		float speedPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f);
		float accelPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).computeEffective(0f);
		stats.getMaxSpeed().modifyPercent(id, speedPercentMod * effectLevel);
		stats.getAcceleration().modifyPercent(id, accelPercentMod * effectLevel);
		stats.getDeceleration().modifyPercent(id, accelPercentMod * effectLevel);

		float speedMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).getMult();
		float accelMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).getMult();
		stats.getMaxSpeed().modifyMult(id, speedMultMod * effectLevel);
		stats.getAcceleration().modifyMult(id, accelMultMod * effectLevel);
		stats.getDeceleration().modifyMult(id, accelMultMod * effectLevel);

		float levelForAlpha = effectLevel;

		if (state == State.IN || state == State.ACTIVE) {
			ship.setPhased(true);
			levelForAlpha = effectLevel;
		} else if (state == State.OUT) {
			ship.setPhased(effectLevel > 0.5f);
			levelForAlpha = effectLevel;
		}

		ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
		ship.setApplyExtraAlphaToEngines(true);

		float shipTimeMult = 1f + (getMaxTimeMult(stats) - 1f) * levelForAlpha;
		stats.getTimeMult().modifyMult(id, shipTimeMult);
		if (player) {
			Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
		} else {
			Global.getCombatEngine().getTimeMult().unmodify(id);
		}
	}


	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}

		Global.getCombatEngine().getTimeMult().unmodify(id);
		stats.getTimeMult().unmodify(id);

		stats.getMaxSpeed().unmodify(id);
		stats.getMaxSpeed().unmodifyMult(id + "_2");
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);

		ship.setPhased(false);
		ship.setExtraAlphaMult(1f);

		ShipSystemAPI cloak = ship.getPhaseCloak();
		if (cloak == null) cloak = ship.getSystem();
		if (cloak != null) {
			((PhaseCloakSystemAPI) cloak).setMinCoilJitterLevel(0f);
		}
	}
}
