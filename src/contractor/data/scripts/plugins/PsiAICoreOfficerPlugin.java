package contractor.data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.*;
import java.util.Random;

import static contractor.data.scripts.util.ContractorStaticVars.DUSKGROUP_PSI_AI_LEADER;

public class PsiAICoreOfficerPlugin extends BaseAICoreOfficerPluginImpl {
	public static final float PSI_MULT = 0.5f;

	@Override
	public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
		float opad = 10f;
		Color text = person.getFaction().getBaseUIColor();
		Color bg = person.getFaction().getDarkUIColor();
		CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(person.getAICoreId());

		tooltip.addSectionHeading("Personality: fearless", text, bg, Alignment.MID, 20);
		String player = Global.getSector().getPlayerPerson().getName().getFirst();
		tooltip.addPara("Has the time of combat-{valor}-[FUN] arrived upon us, my dear " + player + "?", opad);
	}

	@Override
	public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
		String aiCoreIdOverride = "drg_psi_core";
		String factionIdOverride = "player"; //unique core, assume id and faction.

		PersonAPI octInt = Global.getSector().getImportantPeople().getPerson(DUSKGROUP_PSI_AI_LEADER);
		PersonAPI person = Global.getFactory().createPerson();
		person.setFaction(factionIdOverride);
		person.setAICoreId(aiCoreIdOverride);

		person.getStats().setSkipRefresh(true);
		person.setName(new FullName(octInt.getNameString(), "", FullName.Gender.ANY));

		person.setPortraitSprite("graphics/portraits/portrait_contractor_psi_ai2.png");
		person.getStats().setLevel(9);
		person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
		person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
		person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
		person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
		person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
		person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
		person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
		person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
		person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);

		person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_MULT, PSI_MULT);

		person.setPersonality(Personalities.RECKLESS);
		person.setRankId(Ranks.SPACE_CAPTAIN);
		person.setPostId(null);
		person.getStats().setSkipRefresh(false);

		return person;
	}
}
