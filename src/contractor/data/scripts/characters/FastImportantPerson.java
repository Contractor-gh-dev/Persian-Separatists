package contractor.data.scripts.characters;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FastImportantPerson {
	public static PersonAPI createPerson(@Nullable MarketAPI market, @Nullable String id, String faction, @Nullable FullName.Gender gender, @Nullable String RankId, String PostId,
										 PersonImportance personImportance, String firstName, String lastName, @Nullable List<String> tags, String voice,
										 @Nullable List<String> skills, boolean isAdmin, int commDirectoryPosition, boolean isHiddenInCommDirectory,
										 boolean offersMultipleMissions) {
		ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
		WeightedRandomPicker<Object> picker = new WeightedRandomPicker<>();

		PersonAPI person = Global.getFactory().createPerson();
		person.setFaction(faction);
		person.setRankId(RankId);
		person.setPostId(PostId);
		person.setImportance(personImportance);
		person.getName().setFirst(firstName);
		person.getName().setLast(lastName);

		if (id == null)
			person.setId(Misc.genUID());
		else
			person.setId(id);

		if (gender == null) {
			picker.clear();
			picker.add(FullName.Gender.MALE);
			picker.add(FullName.Gender.FEMALE);
			FullName.Gender genderPick = (FullName.Gender) picker.pick();
			person.setGender(genderPick);
		} else
			person.setGender(gender);

		if (RankId == null) {
			picker.clear();
			picker.add(Ranks.SPACE_LIEUTENANT, 2f);
			picker.add(Ranks.SPACE_COMMANDER, 3f);
			picker.add(Ranks.SPACE_CAPTAIN, 2f);
			picker.add(Ranks.SPACE_ADMIRAL, 1f);
			String rank = picker.pick().toString();
			person.setRankId(rank);
		} else
			person.setRankId(RankId);

		if (tags != null)
			for (String tag : tags)
				person.addTag(tag);

		person.setVoice(voice);
		String sprite = Global.getSettings().getSpriteName("characters", person.getId());
		if (sprite != null)
			person.setPortraitSprite(sprite);

		if (skills != null)
			for (String skill : skills)
				person.getStats().setSkillLevel(skill, 1);

		if (market != null) {
			if (isAdmin) {
				market.setAdmin(person);
			}
			market.getCommDirectory().addPerson(person, commDirectoryPosition);
			market.getCommDirectory().getEntryForPerson(person).setHidden(isHiddenInCommDirectory);
			market.addPerson(person);
		}

		ip.addPerson(person);

		if (offersMultipleMissions) {
			person.getMemoryWithoutUpdate().set(BaseMissionHub.NUM_BONUS_MISSIONS, 1);
			BaseMissionHub.set(person, new BaseMissionHub(person));
		}
		return person;
	}
}
