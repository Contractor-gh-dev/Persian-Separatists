package data.missions.militiastandoff;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Planets;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {
	@Override
	public void defineMission(MissionDefinitionAPI api) {
		api.initFleet(FleetSide.PLAYER, "PSM", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "ISS", FleetGoal.ATTACK, true);

		api.addToFleet(FleetSide.PLAYER, "sep_independence_sd", FleetMemberType.SHIP, "PSM Independence", true);
		api.addToFleet(FleetSide.PLAYER, "sep_overlord_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_ship26_nk", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_shomitraile_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_arbiter_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_arbiter_pt", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_mainstay_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_anacreon_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_grizzly_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_starmaster_combat_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_starmaster_carrier_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_starmaster_cargo_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_starmaster_cargo_big_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_starmaster_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_duffie_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_duffie_carrier_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_safeguard_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_safeguard_base_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_fencer_base_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_fencer_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_spectre_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_spectre_sp", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_keystone_sp", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_wellerman_missile_vls", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_wellerman_combat_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_wellerman_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_venom_mk2_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_venom_mk2_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_venom_mk2_true_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_venom_mk2_true_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_venom_mk1_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_venom_mk1_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_comet_sep_sd", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "sep_comet_sep_sd", FleetMemberType.SHIP, false);
		api.defeatOnShipLoss("PSM Independence");

		api.addToFleet(FleetSide.ENEMY, "onslaught_xiv_Elite", FleetMemberType.SHIP, true);
		api.addToFleet(FleetSide.ENEMY, "onslaught_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "legion_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "legion_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "anubis_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "mora_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "mora_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "falcon_xiv_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "falcon_xiv_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "grendel_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "manticore_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "manticore_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "mule_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "mule_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "vanguard_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "vanguard_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "vanguard_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "wolf_hegemony_PD", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "wolf_hegemony_PD", FleetMemberType.SHIP, false);

		api.addBriefingItem("Test PSM ships in a defense scenario");
		api.addBriefingItem("The PSM Independence is both valuable and symbolic to the Separatists and must survive.");
		api.addBriefingItem("PSM ships often counter/are countered by certain design types, assign ships appropriately to succeed");

		api.setFleetTagline(FleetSide.PLAYER, "Persean Separatist Star Navy");
		api.setFleetTagline(FleetSide.ENEMY, "Hegemony Incursion Force");

		float width = 12000f;
		float height = 12000f;
		api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);

		api.addPlanet(0, 0, 256, Planets.BARREN_DESERT, 0, true);
	}
}
