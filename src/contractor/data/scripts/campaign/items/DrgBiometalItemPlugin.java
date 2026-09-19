package contractor.data.scripts.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.DismissDialogDelegate;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.Objects;

public class DrgBiometalItemPlugin extends BaseSpecialItemPlugin {
	public static final String DRG_BIOMETAL_HULLMOD_ID = "contractor_bioarmor";
	public static final String DRG_REPAIR_NANITES_HULLMOD_ID = "contractor_repairnanites";
	public static final String DRG_NANITE_FORGES_HULLMOD_ID = "contractor_naniteforges";

	@Override
	public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
		float pad = 3f;
		float opad = 10f;
		float small = 5f;
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color b = Misc.getButtonTextColor();
		b = Misc.getPositiveHighlightColor();

		if (!Global.CODEX_TOOLTIP_MODE) {
			tooltip.addTitle(getName());
		} else {
			tooltip.addSpacer(-opad);
		}

		String design = getDesignType();
		if (design != null) {
			Misc.addDesignTypePara(tooltip, design, 10f);
		}

		if (!spec.getDesc().isEmpty()) {
			if (Global.CODEX_TOOLTIP_MODE) {
				tooltip.setParaSmallInsignia();
			}
			tooltip.addPara(spec.getDesc(), Misc.getTextColor(), opad);
		}

		addCostLabel(tooltip, opad, transferHandler, stackSource);

		if (!Global.CODEX_TOOLTIP_MODE) {
			if (!Objects.equals(Global.getSector().getPlayerMemoryWithoutUpdate().getBoolean("$canMakeNaniteHM"), true)) {
				tooltip.addPara("Right-click to analyze the " + getName(), b, opad);
			}
		}
	}

	protected String getRightClickRuleTrigger() {
		return "DrgNaniteHullmodItemRC";
	}

	@Override
	public boolean hasRightClickAction() {
		return (!Objects.equals(Global.getSector().getPlayerMemoryWithoutUpdate().getBoolean("$canMakeNaniteHM"), true));
	}

	@Override
	public boolean shouldRemoveOnRightClickAction() {
		return false;
	}

	@Override
	public void performRightClickAction(RightClickActionHelper helper) {
		if (Objects.equals(Global.getSector().getPlayerMemoryWithoutUpdate().getBoolean("$canMakeNaniteHM"), true))
			return;
		Global.getSoundPlayer().playUISound(getSpec().getSoundId(), 1f, 1f);

		RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl(getRightClickRuleTrigger());
		plugin.setCustom1(helper);
		Global.getSector().getCampaignUI().showInteractionDialogFromCargo(plugin,
				Global.getSector().getPlayerFleet(), new DismissDialogDelegate() {
					@Override
					public void dialogDismissed() {
					}
				});
	}
}









