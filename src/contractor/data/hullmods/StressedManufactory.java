package contractor.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class StressedManufactory extends BaseHullMod {
	private static final float REFIT_TIME_PERCENT = 30f;
	//private static final int SAFE_BAY_NUM = -2;
	private static final Map<ShipAPI.HullSize, Integer> SAFE_BAY_NUM = new HashMap<>(); static {
		SAFE_BAY_NUM.put(ShipAPI.HullSize.FRIGATE, 0);
		SAFE_BAY_NUM.put(ShipAPI.HullSize.DESTROYER, 1);
		SAFE_BAY_NUM.put(ShipAPI.HullSize.CRUISER, 3);
		SAFE_BAY_NUM.put(ShipAPI.HullSize.CAPITAL_SHIP, 3);
	}

	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		int bayNum = -SAFE_BAY_NUM.get(ship.getHullSize());
		bayNum += ship.getVariant().getFittedWings().size();
		if (bayNum < 0)
			bayNum = 0;

		ship.getMutableStats().getFighterRefitTimeMult().modifyPercent(id, REFIT_TIME_PERCENT * bayNum);
	}

	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;

		Color h = Misc.getHighlightColor();
		Color b = Misc.getNegativeHighlightColor();

		LabelAPI label = tooltip.addPara("The fighter manufactories on this ship are undersized for the number of flight decks, and can become overwhelmed when at full capacity. " +
						"For every %s launch bay above %s/%s/%s/%s, fighter refit time is increased by %s, depending on hullsize.", opad, h,
				"filled",
				"" + SAFE_BAY_NUM.get(ShipAPI.HullSize.FRIGATE),
				"" + SAFE_BAY_NUM.get(ShipAPI.HullSize.DESTROYER),
				"" + SAFE_BAY_NUM.get(ShipAPI.HullSize.CRUISER),
				"" + SAFE_BAY_NUM.get(ShipAPI.HullSize.CAPITAL_SHIP),
				Math.round(REFIT_TIME_PERCENT) + "%");
		label.setHighlightColors(h, h, h, h, h, b);
		label.setHighlight("filled",
				"" + SAFE_BAY_NUM.get(ShipAPI.HullSize.FRIGATE),
				"" + SAFE_BAY_NUM.get(ShipAPI.HullSize.DESTROYER),
				"" + SAFE_BAY_NUM.get(ShipAPI.HullSize.CRUISER),
				"" + SAFE_BAY_NUM.get(ShipAPI.HullSize.CAPITAL_SHIP)
				, Math.round(REFIT_TIME_PERCENT) + "%");

		if (Global.getSettings().isDevMode() && ship != null) {
			int bayNum = -SAFE_BAY_NUM.get(ship.getHullSize());
			bayNum += ship.getVariant().getFittedWings().size();
			if (bayNum < 0)
				bayNum = 0;

			tooltip.addPara("Debug: debuff is currently:" + (bayNum * REFIT_TIME_PERCENT), opad);
		}
	}
}
