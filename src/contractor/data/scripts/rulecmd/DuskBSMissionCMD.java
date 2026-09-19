package contractor.data.scripts.rulecmd;

import java.util.List;
import java.util.Map;
import java.util.Random;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;

import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;

import com.fs.starfarer.api.loading.PersonMissionSpec;
import com.fs.starfarer.api.util.Misc.Token;

import static contractor.data.scripts.util.ContractorStaticVars.KOIT_ID;

public class DuskBSMissionCMD extends BaseCommandPlugin {

	public static final String TEMP_MISSION_KEY = "$tempMissionKey";
	private static final String id = "dgBSM";

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;

		SectorEntityToken duskPlanet = Global.getSector().getStarSystem(KOIT_ID).getEntityById("duskgroup_planet");
		String missionId = id;

		PersonMissionSpec spec = Global.getSettings().getMissionSpec(missionId);
		if (spec == null) {
			throw new RuntimeException("Mission with spec [" + missionId + "] not found");
		}

		HubMission mission = spec.createMission();

		PersonAPI person = Global.getSector().getImportantPeople().getPerson("duskFactionLeader");

		if (person == null) {
			throw new RuntimeException("Attempting to BeginMission " + missionId +
					" with invalid person.");
		} else {
			mission.setPersonOverride(person);
			//mission.setGenRandom(new Random(Misc.getSalvageSeed(entity)));
			String key = "$beginMission_seedExtra";
			String extra = person.getMemoryWithoutUpdate().getString(key);
			long seed = BarEventManager.getInstance().getSeed(null, person, extra);
			person.getMemoryWithoutUpdate().set(key, "" + seed); // so it's not the same seed for multiple missions
			mission.setGenRandom(new Random(seed));
		}

		mission.createAndAbortIfFailed(duskPlanet.getMarket(), false);

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

