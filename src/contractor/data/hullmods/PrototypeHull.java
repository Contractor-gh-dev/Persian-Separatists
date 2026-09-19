package contractor.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class PrototypeHull extends BaseHullMod {
	private static final float MALFUNCTION_PROB = 0.25f;
	private static final float WEAPON_HEALTH = 0.2f;
	private static final String id = "contractor_prototypehull";

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getWeaponDamageTakenMult().modifyPercent(id, WEAPON_HEALTH);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship.getPeakTimeRemaining() <= 0) {
			MutableShipStatsAPI stats = ship.getMutableStats();
			float effect = stats.getDynamic().getValue(Stats.DMOD_EFFECT_MULT);

			stats.getCriticalMalfunctionChance().modifyPercent(id + ship.getId(), MALFUNCTION_PROB * effect);
			stats.getWeaponMalfunctionChance().modifyPercent(id + ship.getId(), MALFUNCTION_PROB * effect);
		}
	}

	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0)
			return "malfunction";
		if (index == 1)
			return Math.round(WEAPON_HEALTH * 100) + "%";

		return null;
	}
}
