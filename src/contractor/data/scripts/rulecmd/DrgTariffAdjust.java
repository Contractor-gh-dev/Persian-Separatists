package contractor.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

import static contractor.data.scripts.util.ContractorStaticVars.DUSK_ID;
import static contractor.data.scripts.util.ContractorStaticVars.KOIT_ID;

public class DrgTariffAdjust extends BaseCommandPlugin {
	public static final String drgTariffID = "drgTariffID";

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		float tariff = params.get(0).getFloat(memoryMap);

		MarketAPI market = Global.getSector().getStarSystem(KOIT_ID).getEntityById(DUSK_ID).getMarket();
		if (tariff == 0)
			market.getTariff().unmodify(drgTariffID);
		else
			market.getTariff().modifyPercent(drgTariffID, tariff);

		if (dialog == null)
			return true;

		dialog.getTextPanel().setFontSmallInsignia();
		dialog.getTextPanel().addPara("Your new tariff rate is now: " + Math.round(30f + ((0.01f * tariff) * 30f)) + "%",
				Misc.getHighlightColor());
		dialog.getTextPanel().setFontInsignia();

		return true;
	}
}
