package contractor.data.world.dusk;

import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.*;

import static contractor.data.scripts.util.ContractorStaticVars.CONTRACTOR_DRG_ID;
import static contractor.data.scripts.util.ContractorStaticVars.CONTRACTOR_SEP_ID;

public class SeparatistsGen implements SectorGeneratorPlugin {

	public static void initFactionRelationships(SectorAPI sector) {
		FactionAPI contractor_separatists = sector.getFaction(CONTRACTOR_SEP_ID);
		FactionAPI player = sector.getFaction(Factions.PLAYER);
		FactionAPI hegemony = sector.getFaction(Factions.HEGEMONY);
		FactionAPI tritachyon = sector.getFaction(Factions.TRITACHYON);
		FactionAPI pirates = sector.getFaction(Factions.PIRATES);
		FactionAPI independent = sector.getFaction(Factions.INDEPENDENT);
		FactionAPI church = sector.getFaction(Factions.LUDDIC_CHURCH);
		FactionAPI path = sector.getFaction(Factions.LUDDIC_PATH);
		FactionAPI kol = sector.getFaction(Factions.KOL);
		FactionAPI diktat = sector.getFaction(Factions.DIKTAT);
		FactionAPI persean = sector.getFaction(Factions.PERSEAN);
		FactionAPI guard = sector.getFaction(Factions.LIONS_GUARD);
		FactionAPI remnants = sector.getFaction(Factions.REMNANTS);
		FactionAPI derelicts = sector.getFaction(Factions.DERELICT);

		contractor_separatists.setRelationship(player.getId(), -0.1f);
		contractor_separatists.setRelationship(hegemony.getId(), -0.5f);
		contractor_separatists.setRelationship(tritachyon.getId(), -0.2f);
		contractor_separatists.setRelationship(pirates.getId(), -0.8f);
		contractor_separatists.setRelationship(independent.getId(), 0.5f);
		contractor_separatists.setRelationship(persean.getId(), 0.0f);
		contractor_separatists.setRelationship(church.getId(), 0.1f);
		contractor_separatists.setRelationship(path.getId(), -0.5f);
		contractor_separatists.setRelationship(kol.getId(), -0.25f);
		contractor_separatists.setRelationship(diktat.getId(), -0.25f);
		contractor_separatists.setRelationship(guard.getId(), -0.25f);
		contractor_separatists.setRelationship(remnants.getId(), -0.5f);
		contractor_separatists.setRelationship(derelicts.getId(), -0.5f);

		//modded factions
		contractor_separatists.setRelationship(CONTRACTOR_DRG_ID, RepLevel.COOPERATIVE);

		contractor_separatists.setRelationship("SCY", RepLevel.SUSPICIOUS);
		contractor_separatists.setRelationship("shadow_industry", RepLevel.WELCOMING);
		contractor_separatists.setRelationship("syndicate_asp", RepLevel.WELCOMING);

		contractor_separatists.setRelationship("citadeldefenders", RepLevel.FAVORABLE);
		contractor_separatists.setRelationship("tiandong", RepLevel.NEUTRAL);
		contractor_separatists.setRelationship("metelson", RepLevel.FAVORABLE);
		contractor_separatists.setRelationship("Coalition", RepLevel.FAVORABLE);

		contractor_separatists.setRelationship("sun_ice", RepLevel.NEUTRAL);
		contractor_separatists.setRelationship("pn_colony", RepLevel.NEUTRAL);
		contractor_separatists.setRelationship("neutrinocorp", RepLevel.NEUTRAL);
		contractor_separatists.setRelationship("blackrock_driveyards", RepLevel.NEUTRAL);

		contractor_separatists.setRelationship("dassault_mikoyan", RepLevel.SUSPICIOUS);
		contractor_separatists.setRelationship("interstellarimperium", RepLevel.INHOSPITABLE);
		contractor_separatists.setRelationship("apex_design", RepLevel.NEUTRAL);

		contractor_separatists.setRelationship("pack", RepLevel.INHOSPITABLE);
		contractor_separatists.setRelationship("6eme_bureau", RepLevel.INHOSPITABLE);

		contractor_separatists.setRelationship("diableavionics", RepLevel.HOSTILE);
		contractor_separatists.setRelationship("maystar_federationte", RepLevel.HOSTILE);
		contractor_separatists.setRelationship("pirateAnar", RepLevel.HOSTILE);
		contractor_separatists.setRelationship("sun_ici", RepLevel.HOSTILE);
		contractor_separatists.setRelationship("junk_pirates", RepLevel.HOSTILE);
		contractor_separatists.setRelationship("exigency", RepLevel.HOSTILE);
		contractor_separatists.setRelationship("exipirated", RepLevel.HOSTILE);
		contractor_separatists.setRelationship("cabal", RepLevel.HOSTILE);
		contractor_separatists.setRelationship("the_deserter", RepLevel.HOSTILE);
		contractor_separatists.setRelationship("blade_breakers", RepLevel.HOSTILE);

		contractor_separatists.setRelationship("crystanite", RepLevel.VENGEFUL);
		contractor_separatists.setRelationship("new_galactic_order", RepLevel.VENGEFUL);
		contractor_separatists.setRelationship("explorer_society", RepLevel.VENGEFUL);

		contractor_separatists.setRelationship("noir", RepLevel.NEUTRAL);
		contractor_separatists.setRelationship("Lte", RepLevel.NEUTRAL);
		contractor_separatists.setRelationship("GKSec", RepLevel.NEUTRAL);
		contractor_separatists.setRelationship("gmda", RepLevel.SUSPICIOUS);
		contractor_separatists.setRelationship("oculus", RepLevel.NEUTRAL);
		contractor_separatists.setRelationship("nomads", RepLevel.NEUTRAL);
		contractor_separatists.setRelationship("thulelegacy", RepLevel.NEUTRAL);
		contractor_separatists.setRelationship("infected", RepLevel.NEUTRAL);
		contractor_separatists.setRelationship("star_federation", RepLevel.NEUTRAL);
	}

	@Override
	public void generate(SectorAPI sector) {
		SharedData.getData().getPersonBountyEventData().addParticipatingFaction(CONTRACTOR_SEP_ID);
		Separatists_Planets.generate(sector);
		initFactionRelationships(sector);
	}
}
