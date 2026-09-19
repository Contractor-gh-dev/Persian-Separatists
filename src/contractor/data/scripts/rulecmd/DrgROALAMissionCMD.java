package contractor.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.BeginMission;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class DrgROALAMissionCMD extends BaseCommandPlugin {
	public static final String id = "drgROALA";

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;

		if (params.isEmpty()) {
			return true;
		} else {
			String action = params.get(0).getString(memoryMap);
			if ("prep".equals(action)) {
				HubMission mission = (HubMission) Global.getSector().getMemoryWithoutUpdate().get(BeginMission.TEMP_MISSION_KEY);
				dialog.getInteractionTarget().setActivePerson((mission.getPersonOverride()));
				return true;
			}
		}
		return true;
	}
}
