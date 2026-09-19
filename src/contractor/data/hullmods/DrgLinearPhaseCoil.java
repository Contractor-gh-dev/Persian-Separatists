package contractor.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import contractor.data.scripts.shipsystems.DrgLinearPhaseCloakStats;

public class DrgLinearPhaseCoil extends BaseHullMod {
	public static final float PROFILE_MULT = 0.5f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getSensorProfile().modifyMult(id, PROFILE_MULT);
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return Math.round(DrgLinearPhaseCloakStats.MIN_SPEED_MULT * 100) + "%";
		if (index == 1) return Math.round(DrgLinearPhaseCloakStats.BASE_FLUX_LEVEL_FOR_MIN_SPEED * 100) + "%";
		if (index == 2) return Math.round(DrgLinearPhaseCloakStats.MAX_TIME_MULT) + "x";
		if (index == 3) return Math.round(100f - PROFILE_MULT * 100f) + "%";
		return null;
	}
}



