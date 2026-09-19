package data.missions.speedwellmission;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {
	@Override
	public void defineMission(MissionDefinitionAPI api) {
		api.initFleet(FleetSide.PLAYER, "PSM", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "TTS", FleetGoal.ATTACK, true, 10);

		api.addToFleet(FleetSide.PLAYER, "sep_speedwell2_sd", FleetMemberType.SHIP, "PSM Speedwell", true);
		api.addToFleet(FleetSide.PLAYER, "sep_grizzly_cv", FleetMemberType.SHIP, "PSM Theseus", false).getRepairTracker().setCR(0.39f);
		api.addToFleet(FleetSide.PLAYER, "sep_starmaster_cargo_pd", FleetMemberType.SHIP, "PSM Sisyphus", false).getRepairTracker().setCR(0.45f);
		api.addToFleet(FleetSide.PLAYER, "sep_keystone_sp", FleetMemberType.SHIP, false).getRepairTracker().setCR(0.39f);
		api.addToFleet(FleetSide.PLAYER, "sep_venom_mk2_sd", FleetMemberType.SHIP, false).getRepairTracker().setCR(0.39f);
		api.defeatOnShipLoss("PSM Speedwell");

		api.addToFleet(FleetSide.ENEMY, "brilliant_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "scintilla_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "scintilla_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "fulgent_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "fulgent_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "glimmer_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "glimmer_Support", FleetMemberType.SHIP, false);

		api.addBriefingItem("The Speedwell must survive");
		api.addBriefingItem("The Speedwell is the only ship with heavy firepower, use it decisively for successful attacks");
		api.addBriefingItem("Your allies have reduced combat readiness, be aware of their impaired performance");

		api.setFleetTagline(FleetSide.PLAYER, "The Speedwell, and her weary companions");
		api.setFleetTagline(FleetSide.ENEMY, "Unknown Hostiles");

		float width = 12000f;
		float height = 12000f;
		api.initMap(-width/2f, width/2f, -height/2f, height/2f);

		api.setBackgroundSpriteName("graphics/backgrounds/background6.jpg");

		api.addPlugin(new BaseEveryFrameCombatPlugin() {
			public void init(CombatEngineAPI engine) {
				engine.getContext().setStandoffRange(10000f);
			}
		});
	}
}
