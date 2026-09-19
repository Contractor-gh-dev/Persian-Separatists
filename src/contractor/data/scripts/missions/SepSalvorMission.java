package contractor.data.scripts.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;
import java.util.Map;

import static contractor.data.scripts.util.ContractorStaticVars.*;

public class SepSalvorMission extends HubMissionWithSearch {
	public enum Stage {
		SEARCH,
		ClUE1,
		DONE,
		FAIL
	}

	private PersonAPI actor;

	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (!setGlobalReference("$sepSalvorMission_ref")) return false;

		setStoryMission();
		setMissionId("sepSalvorMission");
		setGiverIsPotentialContactOnSuccess(0f);

		actor = Global.getSector().getFaction(CONTRACTOR_SEP_ID).createRandomPerson(FullName.Gender.FEMALE);
		actor.setRankId(Ranks.CITIZEN);
		actor.setPostId(Ranks.POST_CITIZEN);
		actor.setVoice(Voices.FAITHFUL);

		setStartingStage(Stage.SEARCH);
		setSuccessStage(Stage.DONE);
		setFailureStage(Stage.FAIL);

		setRepChanges(25f, -25f, 2f, -5f);
		setCreditReward(500);

		Global.getSoundPlayer().playUISound("ui_intel_log_update", 1, 1);
		return true;
	}

	@Override
	protected boolean callAction(String action, String ruleId, final InteractionDialogAPI dialog,
								 List<Misc.Token> params, final Map<String, MemoryAPI> memoryMap) {

		if ("prepActor".equals(action)) {
			actor.setMarket(dialog.getInteractionTarget().getMarket());
			dialog.getInteractionTarget().setActivePerson(actor);
			if (Global.getSettings().isDevMode())
				dialog.getTextPanel().addPara("prepActor called", Color.GRAY);
			return true;
		}

		if ("manageStage".equals(action)) {
			if (params.size() == 2) {
				boolean goodOrdinal = false;
				SepSalvorMission.Stage stageAssign = Stage.SEARCH;
				int ordinal = params.get(1).getInt(memoryMap);
				for (int i = 0; i < SepSalvorMission.Stage.values().length; i++)
					if (SepSalvorMission.Stage.values()[i].ordinal() == ordinal) {
						goodOrdinal = true;
						stageAssign = SepSalvorMission.Stage.values()[i];
						setCurrentStage(stageAssign, dialog, memoryMap);
						break;
					}
				if (!goodOrdinal) {
					dialog.getTextPanel().addPara("Invalid ordinal: " + ordinal, Misc.getNegativeHighlightColor());
					return false;
				}
				if (Global.getSettings().isDevMode())
					dialog.getTextPanel().addPara("Stage managed to " + stageAssign, Color.GRAY);
				return true;
			} else {
				dialog.getTextPanel().addPara("Invalid number of arguments: " + params.size(), Misc.getNegativeHighlightColor());
				return false;
			}
		}

		return false;
	}
}
