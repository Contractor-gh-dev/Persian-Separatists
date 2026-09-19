package contractor.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class StealthPlating extends BaseHullMod {
	public static final float PROFILE_MULT = 0.25f;
	public static final float DAMAGE_DEBUFF = 10f;
	public static final float SMOD_PROFILE_MULT = 0.08f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		boolean sMod = isSMod(stats);

		stats.getBeamDamageTakenMult().modifyPercent(id, DAMAGE_DEBUFF);
		if (sMod) {
			stats.getSensorProfile().modifyMult(id, SMOD_PROFILE_MULT);
		} else {
			stats.getSensorProfile().modifyMult(id, PROFILE_MULT);
		}
	}

	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();

		tooltip.addPara("Plating that reduces total EM signature via absorption and effectively lowers sensor profile by %s. ", opad, h,
				Math.round(100 * (1f - PROFILE_MULT)) + "%");
		LabelAPI label = tooltip.addPara("However, this comes at the expense of increased vulnerability to thermal weapons like %s, which deal an extra %s damage.", opad, bad,
				"beams",
				(int) DAMAGE_DEBUFF + "%");
		label.setHighlightColors(h, bad);
	}

	public String getSModDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return Math.round((1f - SMOD_PROFILE_MULT) * 100f) + "%";
		return null;
	}
}
