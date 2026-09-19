package contractor.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;

public class GunneryCrews extends BaseHullMod {
	private static final float EMPMOD = 25f;
	private static final float CRMOD = 20f;
	private static final float CREWMOD = 25f;
	private static final float CREWSMOD = 50f;
	private static final float ROFMOD = 10f;

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		boolean sMod = isSMod(stats);
		stats.getEmpDamageTakenMult().modifyPercent(id, -EMPMOD);
		stats.getMinCrewMod().modifyPercent(id, CREWMOD);
		stats.getBallisticRoFMult().modifyPercent(id, ROFMOD);
		stats.getBallisticWeaponFluxCostMod().modifyPercent(id, -ROFMOD);
		stats.getPeakCRDuration().modifyPercent(id, -CRMOD);

		if (sMod)
			stats.getCrewLossMult().modifyPercent(id, CREWSMOD);
		else
			stats.getCrewLossMult().modifyPercent(id, CREWMOD);
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		if (ship.getVariant().hasHullMod("automated"))
			return false;
		List<WeaponAPI> weapons = ship.getAllWeapons();

		for (WeaponAPI w : weapons) {
			if (w.getSlot().getWeaponType() == WeaponAPI.WeaponType.BALLISTIC)
				return true;
		}

		return false;
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().hasHullMod("automated"))
			return "Incompatible with automated ships.";

		List<WeaponAPI> weapons = ship.getAllWeapons();

		for (WeaponAPI w : weapons) {
			if (w.getSlot().getWeaponType() == WeaponAPI.WeaponType.BALLISTIC)
				return null;
		}
		return "Ship requires a ballistic mount.";
	}

	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;

		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();

		tooltip.addPara("Augments weapon systems with onsite magazines, hardened loading systems, and crew compartments to service them. Reduces vulnerability to EMP damage by %s. " +
						"Increases rate of fire of ballistic weapons by %s, and reduces their flux cost by %s.", opad, h,
				Math.round(EMPMOD) + "%", Math.round(ROFMOD) + "%", Math.round(ROFMOD) + "%");
		tooltip.addPara("Increases required crew and crew lost in combat by %s. Crew exhaustion reduces peak combat readiness by %s.", opad, bad,
				Math.round(CREWMOD) + "%", Math.round(CRMOD) + "%");
	}

	public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return Math.round(CREWSMOD) + "%";
		return null;
	}

	public boolean isSModEffectAPenalty() {
		return true;
	}
}
