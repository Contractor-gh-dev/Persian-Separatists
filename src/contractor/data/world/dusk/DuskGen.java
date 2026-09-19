package contractor.data.world.dusk;

import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.*;

import static contractor.data.scripts.util.ContractorStaticVars.CONTRACTOR_DRG_ID;
import static contractor.data.scripts.util.ContractorStaticVars.CONTRACTOR_SEP_ID;

public class DuskGen implements SectorGeneratorPlugin {

	public static void initFactionRelationships(SectorAPI sector) {
		FactionAPI contractor_duskgroup = sector.getFaction(CONTRACTOR_DRG_ID);
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

		contractor_duskgroup.setRelationship(player.getId(), 0.0f);
		contractor_duskgroup.setRelationship(hegemony.getId(), -0.5f);
		contractor_duskgroup.setRelationship(tritachyon.getId(), -0.5f);
		contractor_duskgroup.setRelationship(pirates.getId(), -0.5f);
		contractor_duskgroup.setRelationship(independent.getId(), 0.2f);
		contractor_duskgroup.setRelationship(persean.getId(), -0.2f);
		contractor_duskgroup.setRelationship(church.getId(), -0.5f);
		contractor_duskgroup.setRelationship(path.getId(), -0.8f);
		contractor_duskgroup.setRelationship(kol.getId(), -0.8f);
		contractor_duskgroup.setRelationship(diktat.getId(), -0.1f);
		contractor_duskgroup.setRelationship(guard.getId(), -0.1f);
		contractor_duskgroup.setRelationship(remnants.getId(), -0.5f);
		contractor_duskgroup.setRelationship(derelicts.getId(), -0.5f);

		//modded factions
		contractor_duskgroup.setRelationship(CONTRACTOR_SEP_ID, RepLevel.COOPERATIVE);

		contractor_duskgroup.setRelationship("SCY", RepLevel.SUSPICIOUS);
		contractor_duskgroup.setRelationship("shadow_industry", RepLevel.WELCOMING);
		contractor_duskgroup.setRelationship("syndicate_asp", RepLevel.WELCOMING);

		contractor_duskgroup.setRelationship("citadeldefenders", RepLevel.FAVORABLE);
		contractor_duskgroup.setRelationship("tiandong", RepLevel.NEUTRAL);
		contractor_duskgroup.setRelationship("metelson", RepLevel.FAVORABLE);
		contractor_duskgroup.setRelationship("Coalition", RepLevel.FAVORABLE);

		contractor_duskgroup.setRelationship("sun_ice", RepLevel.NEUTRAL);
		contractor_duskgroup.setRelationship("pn_colony", RepLevel.NEUTRAL);
		contractor_duskgroup.setRelationship("neutrinocorp", RepLevel.NEUTRAL);
		contractor_duskgroup.setRelationship("blackrock_driveyards", RepLevel.SUSPICIOUS);

		contractor_duskgroup.setRelationship("dassault_mikoyan", RepLevel.SUSPICIOUS);
		contractor_duskgroup.setRelationship("interstellarimperium", RepLevel.INHOSPITABLE);
		contractor_duskgroup.setRelationship("apex_design", RepLevel.NEUTRAL);

		contractor_duskgroup.setRelationship("pack", RepLevel.INHOSPITABLE);
		contractor_duskgroup.setRelationship("6eme_bureau", RepLevel.INHOSPITABLE);

		contractor_duskgroup.setRelationship("diableavionics", RepLevel.HOSTILE);
		contractor_duskgroup.setRelationship("maystar_federationte", RepLevel.HOSTILE);
		contractor_duskgroup.setRelationship("pirateAnar", RepLevel.HOSTILE);
		contractor_duskgroup.setRelationship("sun_ici", RepLevel.HOSTILE);
		contractor_duskgroup.setRelationship("junk_pirates", RepLevel.HOSTILE);
		contractor_duskgroup.setRelationship("exigency", RepLevel.HOSTILE);
		contractor_duskgroup.setRelationship("exipirated", RepLevel.HOSTILE);
		contractor_duskgroup.setRelationship("cabal", RepLevel.HOSTILE);
		contractor_duskgroup.setRelationship("the_deserter", RepLevel.HOSTILE);
		contractor_duskgroup.setRelationship("blade_breakers", RepLevel.HOSTILE);

		contractor_duskgroup.setRelationship("crystanite", RepLevel.VENGEFUL);
		contractor_duskgroup.setRelationship("new_galactic_order", RepLevel.VENGEFUL);
		contractor_duskgroup.setRelationship("explorer_society", RepLevel.VENGEFUL);

		contractor_duskgroup.setRelationship("noir", RepLevel.NEUTRAL);
		contractor_duskgroup.setRelationship("Lte", RepLevel.NEUTRAL);
		contractor_duskgroup.setRelationship("GKSec", RepLevel.NEUTRAL);
		contractor_duskgroup.setRelationship("gmda", RepLevel.SUSPICIOUS);
		contractor_duskgroup.setRelationship("oculus", RepLevel.NEUTRAL);
		contractor_duskgroup.setRelationship("nomads", RepLevel.NEUTRAL);
		contractor_duskgroup.setRelationship("thulelegacy", RepLevel.NEUTRAL);
		contractor_duskgroup.setRelationship("infected", RepLevel.NEUTRAL);
		contractor_duskgroup.setRelationship("star_federation", RepLevel.SUSPICIOUS);
	}

	@Override
	public void generate(SectorAPI sector) {
		SharedData.getData().getPersonBountyEventData().addParticipatingFaction(CONTRACTOR_DRG_ID);
		Dusk_Planets.generate(sector);
		initFactionRelationships(sector);
	}
}
