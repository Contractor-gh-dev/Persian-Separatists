package contractor.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

import static contractor.data.scripts.campaign.items.DrgBiometalItemPlugin.*;

public class NaniteCMD extends BaseCommandPlugin {
	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;

		OptionPanelAPI options = dialog.getOptionPanel();
		TextPanelAPI text = dialog.getTextPanel();

		String action = params.get(0).getString(memoryMap);

		MemoryAPI memory = memoryMap.get(MemKeys.LOCAL);
		if (memory == null) return false; // should not be possible unless there are other big problems already

		if ("unlockHullmod".equals(action)) {
			unlockHullmod(dialog, memoryMap);
			return true;
		}
		return false;
	}

	protected void unlockHullmod(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		HullModSpecAPI modSpec = Global.getSettings().getHullModSpec(DRG_BIOMETAL_HULLMOD_ID);
		HullModSpecAPI modSpec2 = Global.getSettings().getHullModSpec(DRG_REPAIR_NANITES_HULLMOD_ID);
		HullModSpecAPI modSpec3 = Global.getSettings().getHullModSpec(DRG_NANITE_FORGES_HULLMOD_ID);

		Global.getSoundPlayer().playUISound("ui_acquired_hullmod", 1, 1);
		TextPanelAPI text = dialog.getTextPanel();
		text.setFontSmallInsignia();

		String str = modSpec.getDisplayName();
		text.addParagraph("Acquired hull mod: " + str, Misc.getPositiveHighlightColor());
		text.highlightInLastPara(Misc.getHighlightColor(), str);

		str = modSpec2.getDisplayName();
		text.addParagraph("Acquired hull mod: " + str, Misc.getPositiveHighlightColor());
		text.highlightInLastPara(Misc.getHighlightColor(), str);

		str = modSpec3.getDisplayName();
		text.addParagraph("Acquired hull mod: " + str, Misc.getPositiveHighlightColor());
		text.highlightInLastPara(Misc.getHighlightColor(), str);

		text.setFontInsignia();

		Global.getSector().getPlayerFaction().addKnownHullMod(DRG_BIOMETAL_HULLMOD_ID);
		Global.getSector().getPlayerFaction().addKnownHullMod(DRG_REPAIR_NANITES_HULLMOD_ID);
		Global.getSector().getPlayerFaction().addKnownHullMod(DRG_NANITE_FORGES_HULLMOD_ID);
	}
}
