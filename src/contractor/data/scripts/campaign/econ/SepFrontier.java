package contractor.data.scripts.campaign.econ;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class SepFrontier extends BaseMarketConditionPlugin {
	public final float MOD = 1f;

	@Override
	public void apply(String id) {
		for (Industry ind : market.getIndustries()) {
			if (ind.getId().equals(Industries.POPULATION)) {
				ind.getDemand(Commodities.LUXURY_GOODS).getQuantity().modifyFlat(id, -MOD);
				ind.getDemand(Commodities.DOMESTIC_GOODS).getQuantity().modifyFlat(id, -MOD);
				ind.getDemand(Commodities.SUPPLIES).getQuantity().modifyFlat(id, MOD);
			}
		}
		market.getDemand(Commodities.HAND_WEAPONS).getDemand().modifyFlat(id, MOD);
	}

	@Override
	public void unapply(String id) {
		for (Industry ind : market.getIndustries()) {
			if (ind.getId().equals(Industries.POPULATION)) {
				ind.getDemand(Commodities.LUXURY_GOODS).getQuantity().unmodify(id);
				ind.getDemand(Commodities.DOMESTIC_GOODS).getQuantity().unmodify(id);
				ind.getDemand(Commodities.SUPPLIES).getQuantity().unmodify(id);
			}
		}
		market.getDemand(Commodities.HAND_WEAPONS).getDemand().unmodify(id);
	}

	@Override
	public void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
		float opad = 10f;

		tooltip.addPara("%s demand for Luxury Goods and Domestic Goods", opad, Misc.getHighlightColor(),
				"-" + Math.round(MOD));
		tooltip.addPara("%s demand for Supplies and Heavy Arms", opad, Misc.getHighlightColor(),
				"+" + Math.round(MOD));
	}
}
