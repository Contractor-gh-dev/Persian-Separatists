package data.missions.drgsimulation;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {
	@Override
	public void defineMission(MissionDefinitionAPI api) {
		api.initFleet(FleetSide.PLAYER, "DRG", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "TTS", FleetGoal.ATTACK, true);

		api.addToFleet(FleetSide.PLAYER, "drg_seraph_sd", FleetMemberType.SHIP, "DRG Throne", true);
		api.addToFleet(FleetSide.PLAYER, "drg_seraph_base_sd", FleetMemberType.SHIP, "DRG Gabriel", true);
		api.addToFleet(FleetSide.PLAYER, "drg_glacier_sd", FleetMemberType.SHIP, "DRG Litch", false);
		api.addToFleet(FleetSide.PLAYER, "drg_glacier_sp", FleetMemberType.SHIP, "DRG Heatdeath", false);
		api.addToFleet(FleetSide.PLAYER, "drg_observer_sd", FleetMemberType.SHIP, "DRG Host", false);
		api.addToFleet(FleetSide.PLAYER, "drg_manta_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "drg_hydra_stock", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "drg_hydra_stock", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_duffie_drg_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "drg_boltzman_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "drg_amp_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_comet_drg_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_comet_drg_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "drg_shard_sd", FleetMemberType.SHIP, false);

		api.addToFleet(FleetSide.ENEMY, "paragon_Elite", FleetMemberType.SHIP, "TTS June", false);
		api.addToFleet(FleetSide.ENEMY, "odyssey_Balanced", FleetMemberType.SHIP, "TTS August", false);
		api.addToFleet(FleetSide.ENEMY, "anubis_Standard", FleetMemberType.SHIP, "TTS Ra", false);
		api.addToFleet(FleetSide.ENEMY, "medusa_Attack", FleetMemberType.SHIP, "TTS Stheno", false);
		api.addToFleet(FleetSide.ENEMY, "medusa_Attack", FleetMemberType.SHIP, "TTS Euryale", false);
		api.addToFleet(FleetSide.ENEMY, "medusa_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "harbinger_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "wolf_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "wolf_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "wolf_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "tempest_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "tempest_Attack", FleetMemberType.SHIP, false);

		api.addBriefingItem("Test DRG ships in a hypothetical defense scenario");

		api.setFleetTagline(FleetSide.PLAYER, "Dusk Research Group Catalog Fleet");
		api.setFleetTagline(FleetSide.ENEMY, "Tri-Tachyon Mercenaries");

		float width = 12000f;
		float height = 12000f;
		api.initMap(-width/2f, width/2f, -height/2f, height/2f);

		api.addPlanet(0, 0, 256, "duskfrozen", 0, true);
	}
}
