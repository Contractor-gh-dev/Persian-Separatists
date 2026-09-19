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

public class HighEmissions extends BaseHullMod {
	private static final float PROFILE_MULT = 1.3f;
	private static final float SMOD_PROFILE_MULT = 1.2f;
	private static final float BUFF = 15f;
	private static final float DEBUFF = 20f;
	private static final float SMOD_DEBUFF = 10f;

	private static final Map<HullSize, Float> DPMOD = new HashMap<>(); static {
		DPMOD.put(ShipAPI.HullSize.FRIGATE, 3f);
		DPMOD.put(ShipAPI.HullSize.DESTROYER, 4f);
		DPMOD.put(ShipAPI.HullSize.CRUISER, 5f);
		DPMOD.put(ShipAPI.HullSize.CAPITAL_SHIP, 6f);
	}


	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		boolean sMod = isSMod(stats);


		stats.getMaxSpeed().modifyPercent(id, BUFF);
		stats.getFluxDissipation().modifyPercent(id, BUFF);
		stats.getFluxCapacity().modifyPercent(id, BUFF);
		stats.getShieldAbsorptionMult().modifyPercent(id, -BUFF);
		stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(id, DPMOD.get(hullSize));
		stats.getPeakCRDuration().modifyPercent(id, -BUFF);


		if (sMod) {
			stats.getSensorProfile().modifyMult(id, SMOD_PROFILE_MULT);
			stats.getSuppliesPerMonth().modifyPercent(id, SMOD_DEBUFF);
			stats.getFuelUseMod().modifyPercent(id, SMOD_DEBUFF);
		} else {
			stats.getSensorProfile().modifyMult(id, PROFILE_MULT);
			stats.getSuppliesPerMonth().modifyPercent(id, DEBUFF);
			stats.getFuelUseMod().modifyPercent(id, DEBUFF);
		}
	}

	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (ship.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES))
			MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), HullMods.SAFETYOVERRIDES, "drg_hereactor");
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		if (ship.getVariant().hasHullMod(HullMods.CIVGRADE) && !ship.getVariant().hasHullMod(HullMods.MILITARIZED_SUBSYSTEMS))
			return false;
		if (ship.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES))
			return false;
		if (ship.getVariant().hasHullMod("drg_lereactor"))
			return false;
		return true;
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().hasHullMod(HullMods.CIVGRADE) && !ship.getVariant().hasHullMod(HullMods.MILITARIZED_SUBSYSTEMS))
			return "Incompatible with non-militarized civilian-grade hulls.";
		if (ship.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES))
			return "Incompatible with Safety Overrides.";
		if (ship.getVariant().hasHullMod("drg_lereactor"))
			return "Incompatible with Low Emission Reactor.";
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

		tooltip.addPara("Reactor modifications to boost output for better performance. Increases max speed, flux capacity, flux dissipation, and shield absorption by %s each.", opad, h,
				"15%");
		LabelAPI label = tooltip.addPara("Increases ships %s by %s/%s/%s/%s depending on hull size. Increases sensor profile by %s, and raises supply and fuel use by %s. Reduces peak performance time by %s.", opad, bad,
				"DP", "3", "4", "5", "6", "30%", "20%", "15%");
		label.setHighlightColors(blue, bad, bad, bad, bad, bad, bad, bad);
		label.setHighlight("DP", "3", "4", "5", "6", "30%", "20%", "15%");
	}

	public String getSModDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "20%";
		if (index == 1) return Math.round(SMOD_DEBUFF) + "%";
		return null;
	}
}

