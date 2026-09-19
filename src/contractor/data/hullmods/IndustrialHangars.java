package contractor.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;

public class IndustrialHangars extends BaseHullMod {
	private static final float PILOT_LOSS = 50f;
	private static final float FIGHTER_COST = 25f;
	private static final float RATE_MODIFIER = 0.6f;

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(Stats.FIGHTER_COST_MOD).modifyPercent(id, -FIGHTER_COST);
		stats.getDynamic().getMod(Stats.BOMBER_COST_MOD).modifyPercent(id, -FIGHTER_COST);
		stats.getDynamic().getMod(Stats.INTERCEPTOR_COST_MOD).modifyPercent(id, -FIGHTER_COST);
		stats.getDynamic().getMod(Stats.SUPPORT_COST_MOD).modifyPercent(id, -FIGHTER_COST);
		stats.getDynamic().getStat(Stats.FIGHTER_CREW_LOSS_MULT).modifyPercent(id, -PILOT_LOSS);
	}

	public void advanceInCombat(ShipAPI ship, float amount) {
		List<FighterLaunchBayAPI> bays = ship.getLaunchBaysCopy();
		if (bays.isEmpty())
			return;

		for (FighterLaunchBayAPI bay : bays) {
			if (bay.getWing() == null)
				continue;
			if (bay.getCurrRate() < RATE_MODIFIER)
				bay.setCurrRate(RATE_MODIFIER);
		}
	}

	@Override
	public boolean affectsOPCosts() {
		return true;
	}

	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;

		Color h = Misc.getHighlightColor();
		Color b = Misc.getNegativeHighlightColor();

		LabelAPI label = tooltip.addPara("Industrial Hangars designed to support a vast number of light craft, with accompanying manufactories and support infrastructure." +
						"Wing OP cost is reduced by %s and reduces fighter pilot casualties by %s. Further more, the minimum fighter replacement rate cannot go below %s.", opad, h,
				Math.round(FIGHTER_COST) + "%", Math.round(PILOT_LOSS) + "%", Math.round(RATE_MODIFIER * 100) + "%");
	}
}
