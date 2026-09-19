package contractor.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONException;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class LowEmissions extends BaseHullMod {
	private static final float PROFILE_MULT = 0.4f;
	private static final float SMOD_PROFILE_MULT = 0.2f;
	private static final float DEBUFF = -20f;
	private static final float SMOD_DEBUFF = -10f;

	private static final Map<HullSize, Float> DPMOD = new HashMap<>(); static {
		DPMOD.put(ShipAPI.HullSize.FRIGATE, -2f);
		DPMOD.put(ShipAPI.HullSize.DESTROYER, -3f);
		DPMOD.put(ShipAPI.HullSize.CRUISER, -4f);
		DPMOD.put(ShipAPI.HullSize.CAPITAL_SHIP, -5f);
	}


	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		boolean sMod = isSMod(stats);

		stats.getSuppliesPerMonth().modifyPercent(id, DEBUFF);
		stats.getFuelUseMod().modifyPercent(id, DEBUFF);
		stats.getPeakCRDuration().modifyPercent(id, -SMOD_DEBUFF);

		stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(id, (float) DPMOD.get(hullSize)); //might need below zero sanity check

		if (sMod) {
			stats.getSensorProfile().modifyMult(id, SMOD_PROFILE_MULT);
			stats.getMaxSpeed().modifyPercent(id, SMOD_DEBUFF);
			stats.getFluxDissipation().modifyPercent(id, SMOD_DEBUFF);
			stats.getFluxCapacity().modifyPercent(id, SMOD_DEBUFF);
			stats.getShieldAbsorptionMult().modifyPercent(id, -SMOD_DEBUFF);
		} else {
			stats.getSensorProfile().modifyMult(id, PROFILE_MULT);
			stats.getMaxSpeed().modifyPercent(id, DEBUFF);
			stats.getFluxDissipation().modifyPercent(id, DEBUFF);
			stats.getFluxCapacity().modifyPercent(id, DEBUFF);
			stats.getShieldAbsorptionMult().modifyPercent(id, -DEBUFF);
		}
	}

	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (ship.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES))
			MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), HullMods.SAFETYOVERRIDES, "drg_lereactor");
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		if (ship.getVariant().hasHullMod(HullMods.CIVGRADE) && !ship.getVariant().hasHullMod(HullMods.MILITARIZED_SUBSYSTEMS))
			return false;
		if (ship.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES))
			return false;
		if (ship.getVariant().hasHullMod("drg_hereactor"))
			return false;
		return true;
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().hasHullMod(HullMods.CIVGRADE) && !ship.getVariant().hasHullMod(HullMods.MILITARIZED_SUBSYSTEMS))
			return "Incompatible with non-militarized civilian-grade hulls.";
		if (ship.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES))
			return "Incompatible with Safety Overrides.";
		if (ship.getVariant().hasHullMod("drg_hereactor"))
			return "Incompatible with High Emission Reactor.";
		return null;
	}

	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;

		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();
		Color blue;
		try {
			blue = Misc.getColor(Global.getSettings().getSettingsJSON(), "mountBlueColor");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		LabelAPI label = tooltip.addPara("Reactor modifications to lower output for better logistics. Reduces ships %s by %s/%s/%s/%s depending on hull size. Lowers sensor profile by %s, and reduces supply and fuel use by %s. " +
						"Peak performance time increased by %s", opad, h,
				"DP", "2", "3", "4", "5", "60%", "10%", "10%");
		label.setHighlightColors(blue, h, h, h, h, h, h, h);
		label.setHighlight("DP", "2", "3", "4", "5", "60%", "10%", "10%");

		tooltip.addPara("Lowers max speed, flux capacity, flux dissipation, and shield absorption by %s each.", opad, bad, "20%");
	}

	public String getSModDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return (int) -SMOD_DEBUFF + "%";
		return null;
	}
}
