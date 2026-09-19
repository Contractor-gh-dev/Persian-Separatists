package contractor.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

import java.util.List;
import java.util.Map;

public class DamagePlayerShip extends BaseCommandPlugin {

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;

		FleetMemberAPI flagship = Global.getSector().getPlayerFleet().getFlagship();
		FleetMemberAPI bystander;

		boolean flagshipFlag;
		boolean phaseOverride;
		boolean bystanderFlag;

		if (params.isEmpty()) {    //is same as true, true, false
			doDamage(flagship);
			addFlagshipNormText(dialog.getTextPanel());
			addShipDamagedText(flagship, dialog.getTextPanel());
			return true;
		}

		flagshipFlag = params.get(0).getBoolean(memoryMap);
		phaseOverride = params.get(1).getBoolean(memoryMap);
		bystanderFlag = params.get(2).getBoolean(memoryMap);


		if (flagship.isPhaseShip()) { //check if player flagship is phase and act
			if (flagshipFlag && phaseOverride) { //if ignore phase protection
				doDamage(flagship);
				addFlagshipPhaseOvrdText(dialog.getTextPanel());
				addShipDamagedText(flagship, dialog.getTextPanel());
				return true;
			} else if (flagshipFlag) { //if allow phase protection
				doPhaseSound();
				addPhaseText(dialog.getTextPanel());
				if (bystanderFlag) {
					bystander = getBystander();
					if (bystander != null) {//check if bystander gets hit
						doDamage(bystander);
						addBystanderText(dialog.getTextPanel());
						addShipDamagedText(bystander, dialog.getTextPanel());
					}
				}
				return true;
			}
		} else {
			if (flagshipFlag) {    //need to damage player?
				doDamage(flagship);
				addFlagshipNormText(dialog.getTextPanel());
				addShipDamagedText(flagship, dialog.getTextPanel());
				return true;
			} else if (bystanderFlag) { //need to damage fleet member?
				bystander = getBystander();
				if (bystander != null) {
					doDamage(bystander);
					addBystanderForceText(dialog.getTextPanel());
					addShipDamagedText(bystander, dialog.getTextPanel());
				}
			}
		}
		return false; //will happen if error or false, false, false
	}

	private static void doDamage(FleetMemberAPI ship) {
		float damageMult = ship.getDeployCost();
		if (damageMult < 1) damageMult = 1;
		Misc.applyDamage(ship, null, damageMult, true, "drg_blacksite", "DRG Blacksite",
				false, null, "");
		Global.getSoundPlayer().playUISound("sep_damageplayership", 1, 0.75f);
	}

	private static FleetMemberAPI getBystander() {
		List<FleetMemberAPI> memberList = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
		FleetMemberAPI tester = null;

		for (FleetMemberAPI member : memberList) {
			if (!member.isPhaseShip())
				tester = member;
			break;
		}

		return tester;
	}

	private static void doPhaseSound() {
		Global.getSoundPlayer().playUISound("ship_warp_out", 1, 1);
	}

	private static void addShipDamagedText(FleetMemberAPI member, TextPanelAPI text) {
		text.setFontSmallInsignia();
		text.addParagraph(member.getShipName() + " has been damaged!", Misc.getNegativeHighlightColor());
		text.highlightInLastPara(Misc.getHighlightColor(), member.getShipName());
		text.setFontInsignia();
	}

	private static void addFlagshipNormText(TextPanelAPI text) {
		text.setFontInsignia();
		text.addParagraph("An explosion rocks your flagship, overloading the shields, and the remaining energy crushes a section of hull. Damage control teams make their way to the area and begin assessment.");
	}

	private static void addFlagshipPhaseOvrdText(TextPanelAPI text) {
		text.setFontInsignia();
		text.addParagraph("You give the order to phase shift, but it seems as though a curse of Ludd has befallen the ship, and the drive fails to activate in time.");
		text.addParagraph("An explosion rocks your flagship, crushing a section of hull. Damage control teams make their way to the area and begin assessment.");
	}

	private static void addBystanderText(TextPanelAPI text) {
		text.setFontInsignia();
		text.addParagraph("However, an unfortunate member of your fleet was positioned behind your flagship and is struck, the explosion crushes a section of hull.");
	}

	private static void addBystanderForceText(TextPanelAPI text) {
		text.setFontInsignia();
		text.addParagraph("An unfortunate member of your fleet is struck by the projectile, the explosion crushes a section of hull.");
	}

	private static void addPhaseText(TextPanelAPI text) {
		text.setFontInsignia();
		text.addParagraph("Moments before impact, your ship phases into p-space and the projectile harmlessly passes through.");
	}
}
