package contractor.data.scripts.campaign.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.SkillPickPreference;
import com.fs.starfarer.api.impl.campaign.fleets.SDFBase;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import org.lwjgl.util.vector.Vector2f;

import static contractor.data.scripts.util.ContractorStaticVars.CONTRACTOR_SEP_ID;

public class SDFSeparatists2 extends SDFBase {
	public SDFSeparatists2() {
	}

	@Override
	protected String getFactionId() {
		return CONTRACTOR_SEP_ID;
	}

	protected SkillPickPreference getCommanderShipSkillPreference() {
		return SkillPickPreference.NO_ENERGY_YES_BALLISTIC_YES_MISSILE_YES_DEFENSE;
	}

	@Override
	protected MarketAPI getSourceMarket() {
		return Global.getSector().getEconomy().getMarket("separatists_iapidi_market");
	}

	@Override
	protected String getDefeatTriggerToUse() {
		return "SDFSeparatists2Defeated";
	}

	@Override
	public boolean canSpawnFleetNow() {
		MarketAPI source = getSourceMarket();
		if (source == null || source.hasCondition(Conditions.DECIVILIZED)) return false;
		if (!source.hasIndustry("sep_orbitalstation_small")) return false;
		if (!source.getFactionId().equals(getFactionId())) return false;
		return true;
	}

	@Override
	public CampaignFleetAPI spawnFleet() {
		MarketAPI iapidi = getSourceMarket();
		Vector2f loc = iapidi.getLocationInHyperspace();

		FleetCreatorMission m = new FleetCreatorMission(random);

		m.beginFleet();

		m.triggerCreateFleet(FleetSize.MAXIMUM, FleetQuality.SMOD_1, getFactionId(), FleetTypes.PATROL_LARGE, loc);

		m.triggerSetFleetSizeFraction(0.6f);

		m.triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.DEFAULT);
		m.triggerSetFleetDoctrineComp(3, 2, 0);
		m.triggerSetFleetCommander(getPerson());

		m.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1);
		m.triggerFleetAddCommanderSkill(Skills.TACTICAL_DRILLS, 1);
		m.triggerFleetAddCommanderSkill(Skills.CREW_TRAINING, 1);
		m.triggerFleetAddCommanderSkill(Skills.CARRIER_GROUP, 1);
		m.triggerFleetAddCommanderSkill(Skills.FIGHTER_UPLINK, 1);

		m.triggerSetPatrol();
		m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_SOURCE_MARKET, iapidi);
		m.triggerFleetSetName("Star Navy 2nd Battalion");
		m.triggerPatrolAllowTransponderOff();
		m.triggerOrderFleetPatrol(iapidi.getStarSystem());

		CampaignFleetAPI fleet = m.createFleet();
		fleet.removeScriptsOfClass(MissionFleetAutoDespawn.class);
		iapidi.getContainingLocation().addEntity(fleet);
		fleet.setLocation(iapidi.getPlanetEntity().getLocation().x, iapidi.getPlanetEntity().getLocation().y);
		fleet.setFacing(random.nextFloat() * 360f);

		boolean hasOverlord = false;

		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			if (member.getHullId().equals("sep_overlord")) {
				hasOverlord = true;
			} else if (member.getHullId().equals("sep_anacreon")) {
				member.setVariant(getVariant("sep_anacreon_vls"), false, false);
			} else if (member.getHullId().equals("sep_keystone")) {
				member.setVariant(getVariant("sep_keystone_as"), false, false);
			} else if (member.getHullId().equals("sep_starmaster_carrier")) {
				if (random.nextFloat() < 0.5f) {
					member.setVariant(getVariant("sep_starmaster_carrier_as"), false, false);
				} else {
					member.setVariant(getVariant("sep_starmaster_carrier_bm"), false, false);
				}
			} else if (member.getHullId().equals("sep_mainstay")) {
				if (random.nextFloat() < 0.2f) {
					member.setVariant(getVariant("sep_mainstay_nk"), false, false);
				} else {
					member.setVariant(getVariant("sep_mainstay_sd"), false, false);
				}
			} else if (member.getHullId().equals("sep_arbiter")) {
				member.setVariant(getVariant("sep_arbiter_pt"), false, false);
			}
		}

		if (!hasOverlord) {
			fleet.getFleetData().addFleetMember("sep_overlord_sd");
		}

		return fleet;
	}
}




