package contractor.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.loading.PersonMissionSpec;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class SepSalvorMissionCMD extends BaseCommandPlugin {
	public static final String id = "sepSalvorMission";

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;

		MarketAPI market = dialog.getInteractionTarget().getMarket();
		String missionId = id;

		PersonMissionSpec spec = Global.getSettings().getMissionSpec(missionId);
		if (spec == null) {
			throw new RuntimeException("Mission with spec [" + missionId + "] not found");
		}


		HubMission mission = spec.createMission();

		PersonAPI person = Global.getSector().getPlayerPerson();

		if (person == null) {
			throw new RuntimeException("Attempting to BeginMission " + missionId +
					" with invalid person.");
		} else {
			String key = "$beginMission_seedExtra";
			String extra = person.getMemoryWithoutUpdate().getString(key);
			long seed = BarEventManager.getInstance().getSeed(null, person, extra);
			mission.setGenRandom(new Random(seed));
		}

		mission.createAndAbortIfFailed(market, false);

		if (mission.isMissionCreationAborted()) {
			MemoryAPI memory = memoryMap.get(MemKeys.LOCAL);
			if (memory == null && dialog.getInteractionTarget() != null) {
				if (dialog.getInteractionTarget().getActivePerson() != null) {
					memory = dialog.getInteractionTarget().getActivePerson().getMemoryWithoutUpdate();
				} else {
					memory = dialog.getInteractionTarget().getMemoryWithoutUpdate();
				}
				if (memory != null) {
					memory.set("$missionCreationFailed", mission.getMissionId());
				}
			}
			return false;
		}

		mission.accept(dialog, memoryMap);

		return true;
	}
}
