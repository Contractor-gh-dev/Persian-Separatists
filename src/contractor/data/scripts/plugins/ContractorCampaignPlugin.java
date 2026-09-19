package contractor.data.scripts.plugins;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;

import static contractor.data.scripts.util.ContractorStaticVars.CONTRACTOR_PSI_CORE_ID;

public class ContractorCampaignPlugin extends BaseCampaignPlugin {

	@Override
	public String getId() {
		return "contractor_campaignPlugin";
	}

	public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
		if (CONTRACTOR_PSI_CORE_ID.equals(commodityId)) {
			return new PluginPick<>(new PsiAICoreOfficerPlugin(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
		}
		return null;
	}
}
