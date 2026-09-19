package contractor.data.scripts.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;
import java.util.Map;

import static contractor.data.scripts.util.ContractorStaticVars.CONTRACTOR_DRG_ID;
import static contractor.data.scripts.util.ContractorStaticVars.FENAR_FACTION_LEAD;

public class sepFenarPlanetMission extends HubMissionWithSearch {
	public enum Stage {
		AWARE,
		SEARCH,
		RELOCATE,
		KILL,
		DONE
	}

	protected StarSystemAPI fenarSystem;
	protected SectorEntityToken fenarPlanet;
	protected PersonAPI fenarLeader, DRGRep;

	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (!setGlobalReference("$sepFenarPlanetMission_ref")) return false;

		setStoryMission();
		setNoAbandon();
		setMissionId("sepFenarPlanetMission");
		setGiverIsPotentialContactOnSuccess(0f);

		DRGRep = Global.getSector().getFaction(CONTRACTOR_DRG_ID).createRandomPerson();
		DRGRep.setRankId(Ranks.AGENT);
		DRGRep.setPostId(Ranks.POST_AGENT);
		DRGRep.setVoice(Voices.SPACER);

		fenarSystem = Global.getSector().getStarSystem("Fenar");
		fenarPlanet = fenarSystem.getEntityById("fenar_fenar");
		fenarLeader = Global.getSector().getImportantPeople().getPerson(FENAR_FACTION_LEAD);
		fenarLeader.setMarket(fenarPlanet.getMarket());

		if (fenarLeader == null)
			return false;

		setStartingStage(Stage.AWARE);
		setSuccessStage(Stage.DONE);
		setFailureStage(Stage.KILL);

		setRepPersonChangesNone();
		setRepFactionChangesNone();

		setRepPenaltyPerson(0f);
		setRepPenaltyFaction(0f);

		Global.getSoundPlayer().playUISound("ui_intel_log_update", 1, 1);
		return true;
	}

	@Override
	protected boolean callAction(String action, String ruleId, final InteractionDialogAPI dialog,
								 List<Misc.Token> params, final Map<String, MemoryAPI> memoryMap) {

		if ("prepDRGRep".equals(action)) {
			dialog.getInteractionTarget().setActivePerson(DRGRep);
			if (Global.getSettings().isDevMode())
				dialog.getTextPanel().addPara("prepDRGRep called", Color.GRAY);
			return true;
		}

		if ("manageStage".equals(action)) {
			if (params.size() == 2) {
				boolean goodOrdinal = false;
				Stage stageAssign = Stage.AWARE;
				int ordinal = params.get(1).getInt(memoryMap);
				for (int i = 0; i < Stage.values().length; i++)
					if (Stage.values()[i].ordinal() == ordinal) {
						goodOrdinal = true;
						stageAssign = Stage.values()[i];
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

		if ("cleanup".equals(action)) {
			MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
			memory.unset("$exploredStationFenarLoner");
			memory.unset("$metPlanetFenarDRGRep");
			return true;
		}

		return super.callAction(action, ruleId, dialog, params, memoryMap);
	}

	protected void updateInteractionDataImpl() {
		set("$sepFenarPlanetMission_stage", getCurrentStage());
	}

	// description when selected in intel screen
	@Override
	public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();

		if (currentStage == Stage.AWARE)
			info.addPara("I found a strange body of an alien in an abandoned research station. There must be more info about this, I'll have to search around or maybe even ask someone.", opad);
		if (currentStage == Stage.SEARCH)
			info.addPara("A DRG hireling told me that there were more aliens were alive, and originated from a lost planet near the Verdan system.", opad);
	}

	// short description in popups and the intel entry
	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		Color h = Misc.getHighlightColor();

		if (currentStage == Stage.AWARE)
			info.addPara("I found a body of an alien in a research station.", tc, pad);
		if (currentStage == Stage.SEARCH)
			info.addPara("I need to search near the Verdan system for a lost planet.", tc, pad);

		return false;
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		if (currentStage == Stage.SEARCH && Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().contains("$foundPlanetFenarClue"))
			return Global.getSector().getStarSystem("Fenar").getHyperspaceAnchor();
		if (currentStage == Stage.SEARCH)
			return Global.getSector().getStarSystem("Verdan").getHyperspaceAnchor();
		return null;
	}

	@Override
	public String getBaseName() {
		return "Dogs of War";
	}
}
